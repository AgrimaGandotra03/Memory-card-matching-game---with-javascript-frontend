package com.memorygame.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorygame.dto.CardDto;
import com.memorygame.dto.GameSessionResponse;
import com.memorygame.dto.HintResponse;
import com.memorygame.dto.ScoreRecordResponse;
import com.memorygame.model.*;
import com.memorygame.repository.GameSessionRepository;
import com.memorygame.repository.PerformanceHistoryRepository;
import com.memorygame.repository.ScoreRecordRepository;
import com.memorygame.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core game engine.
 *
 * Key design decisions:
 *
 * 1. Board as JSON string
 *    The full List<CardState> is stored in a single TEXT column (boardStateJson)
 *    and deserialized with Jackson on each operation. This avoids a join table and
 *    keeps the schema simple — acceptable for a prototype where boards are ≤ 64 cards.
 *
 * 2. Server-side timer
 *    elapsedSeconds is always computed as:
 *      now − startedAt − totalPausedSeconds
 *    The client sends no time values. On pause, pausedAt is recorded; on resume,
 *    the gap is accumulated into totalPausedSeconds.
 *
 * 3. Turn state in a column
 *    firstFlippedCardId (-1 = none) is stored as a plain column on GameSession
 *    so the backend knows which card is "face-up waiting" between HTTP requests
 *    (stateless REST — no server-side session / HttpSession).
 *
 * 4. Score formula
 *    score = max(0, baseScore − moves×2 − hintsUsed×50 − elapsedSeconds×1)
 *    Constants are defined below and easy to tune.
 */
@Service
public class GameService {

    // ── Score formula constants ──────────────────────────────────────────────

    private static final Map<Difficulty, Integer> BASE_SCORE = Map.of(
            Difficulty.EASY,   500,
            Difficulty.MEDIUM, 1000,
            Difficulty.HARD,   2000
    );
    private static final int MOVES_PENALTY = 2;   // per move
    private static final int HINT_PENALTY  = 50;  // per hint
    private static final int TIME_PENALTY  = 1;   // per elapsed second

        private static final Map<Difficulty, Integer> BASE_MOVE_LIMIT_SECONDS = Map.of(
            Difficulty.EASY, 10, Difficulty.MEDIUM, 8, Difficulty.HARD, 6);
        private static final Map<Difficulty, Integer> FOCUS_MOVE_LIMIT_SECONDS = Map.of(
            Difficulty.EASY, 6, Difficulty.MEDIUM, 5, Difficulty.HARD, 4);

    // ── Difficulty → pair counts ──────────────────────────────────────────────

    private static final Map<Difficulty, Integer> PAIRS = Map.of(
            Difficulty.EASY,   8,   // 4×4  = 16 cards
            Difficulty.MEDIUM, 18,  // 6×6  = 36 cards
            Difficulty.HARD,   32   // 8×8  = 64 cards
    );

    // ── Theme symbol pools (each must have ≥ 32 unique symbols) ──────────────

    private static final Map<String, List<String>> THEME_SYMBOLS = new LinkedHashMap<>();
    static {
        THEME_SYMBOLS.put("animals", Arrays.asList(
            "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯",
            "🦁","🐮","🐷","🐸","🐙","🦋","🦄","🐝","🦅","🐬",
            "🐳","🦈","🦒","🦓","🐘","🦏","🦛","🦧","🦥","🦦","🦘","🦬"
        ));
        THEME_SYMBOLS.put("fruits", Arrays.asList(
            "🍎","🍊","🍋","🍇","🍓","🍒","🍑","🥭","🍍","🥝",
            "🍅","🥑","🫐","🍈","🍐","🍌","🍉","🍏","🍆","🥦",
            "🥕","🌽","🥜","🍄","🌰","🥥","🍯","🫑","🌶","🧅","🧄","🍞"
        ));
        THEME_SYMBOLS.put("space", Arrays.asList(
            "🌟","⭐","🌙","☀","🪐","🌍","🌎","🌏","🌑","🌒",
            "🌓","🌔","🌕","🌖","🌗","🌘","🌚","🌛","🌜","🌝",
            "🛸","🚀","🛰","☄","🌌","🔭","🌠","💫","✨","🌈","⚡","🎇"
        ));
        THEME_SYMBOLS.put("flags", Arrays.asList(
            "🇺🇸","🇬🇧","🇫🇷","🇩🇪","🇮🇹","🇯🇵","🇨🇳","🇧🇷","🇮🇳","🇨🇦",
            "🇦🇺","🇲🇽","🇰🇷","🇷🇺","🇿🇦","🇪🇸","🇵🇹","🇸🇪","🇳🇴","🇫🇮",
            "🇩🇰","🇳🇱","🇨🇭","🇦🇹","🇧🇪","🇵🇱","🇬🇷","🇹🇷","🇦🇷","🇵🇭","🇮🇩","🇻🇳"
        ));
    }

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final GameSessionRepository sessionRepository;
    private final PerformanceHistoryRepository performanceHistoryRepository;
    private final ScoreRecordRepository scoreRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final int previewDurationSeconds;

    @Autowired
    public GameService(GameSessionRepository sessionRepository,
                       PerformanceHistoryRepository performanceHistoryRepository,
                       ScoreRecordRepository scoreRepository,
                       UserRepository userRepository,
                       ObjectMapper objectMapper,
                       @Value("${memorygame.preview-duration-seconds:4}") int previewDurationSeconds) {
        this.sessionRepository = sessionRepository;
        this.performanceHistoryRepository = performanceHistoryRepository;
        this.scoreRepository   = scoreRepository;
        this.userRepository    = userRepository;
        this.objectMapper      = objectMapper;
        this.previewDurationSeconds = Math.max(0, previewDurationSeconds);
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Creates a new shuffled game for the given user, difficulty, and theme.
     * The returned board has symbolKey=null for every card (all hidden).
     */
    @Transactional
    public GameSessionResponse startNewGame(Long userId, Difficulty difficulty, String theme) {
        return startNewGame(userId, difficulty, theme, false);
    }

    @Transactional
    public GameSessionResponse startNewGame(Long userId, Difficulty difficulty, String theme,
                                            boolean focusMode) {
        User user = findUser(userId);
        String resolvedTheme = resolveTheme(theme);
        List<CardState> board = generateBoard(difficulty, resolvedTheme);
        Instant startedAt = Instant.now();

        GameSession session = new GameSession();
        session.setUser(user);
        session.setDifficulty(difficulty);
        session.setTheme(resolvedTheme);
        session.setBoardStateJson(toJson(board));
        session.setStatus(GameStatus.ACTIVE);
        session.setStartedAt(startedAt);
        session.setMoves(0);
        session.setHintsUsed(0);
        session.setScore(0);
        session.setAverageReactionTimeMillis(0L);
        session.setMistakeCount(0);
        session.setCurrentConsecutiveMatchStreak(0);
        session.setMaxConsecutiveMatchStreak(0);
        session.setConcentrationScore(100.0);
        session.setAccuracyPercent(0.0);
        session.setPreviewEndsAt(startedAt.plusSeconds(previewDurationSeconds));
        session.setFirstFlipAt(null);
        session.setMoveDeadlineAt(null);
        session.setFocusMode(focusMode);
        session.setMoveTimeLimitSeconds(initialMoveTimeLimit(difficulty, focusMode));
        session.setFirstFlippedCardId(-1);
        session.setTotalPausedSeconds(0L);

        session = sessionRepository.save(session);

        GameSessionResponse resp = buildResponse(session, board);
        resp.setMessage(String.format(
                "Game started! Difficulty: %s | Theme: %s | Pairs to match: %d",
                difficulty, resolvedTheme, board.size() / 2));
        return resp;
    }

    /**
     * Flips a card in the current session.
     *
     * State machine:
     *   • First flip  → card goes face-up; wait for second flip.
     *   • Second flip → compare symbols:
     *       MATCH   : both cards stay face-up and are permanently matched.
     *       NO_MATCH: both cards are flipped back face-down.
     *   • If all pairs are now matched, status → WON and a ScoreRecord is written.
     */
    @Transactional
    public GameSessionResponse flipCard(Long sessionId, int cardId) {
        GameSession session = requireActive(sessionId);
        List<CardState> board = fromJson(session.getBoardStateJson());

        if (isPreviewActive(session)) {
            throw new IllegalStateException("Memory preview is still active. Wait until it ends before flipping.");
        }

        CardState card = requireCard(board, cardId);
        if (card.isMatched()) {
            throw new IllegalArgumentException("Card " + cardId + " is already matched — choose another.");
        }
        if (card.isFlipped()) {
            throw new IllegalArgumentException("Card " + cardId + " is already face-up — wait for the second flip.");
        }

        String flipResult;
        boolean wonGame = false;
        int firstId = session.getFirstFlippedCardId();
        List<Integer> revealedThisMove;

        if (firstId == -1) {
            // ── First flip of the turn ──────────────────────────────────────
            card.setFlipped(true);
            session.setFirstFlippedCardId(cardId);
            Instant firstFlipAt = Instant.now();
            session.setFirstFlipAt(firstFlipAt);
            session.setMoveDeadlineAt(firstFlipAt.plusSeconds(session.getMoveTimeLimitSeconds()));
            flipResult = "FIRST_FLIP";
            revealedThisMove = List.of(cardId);

        } else {
            // ── Second flip of the turn ─────────────────────────────────────
            CardState firstCard = requireCard(board, firstId);
            if (session.isFocusMode() && isMoveExpired(session)) {
                expireTimedOutMove(session, board, firstCard);
                throw new IllegalStateException("Focus-mode move timer expired. Choose a new first card.");
            }
            Instant firstFlipAt = session.getFirstFlipAt();
            card.setFlipped(true);
            session.setMoves(session.getMoves() + 1);
            revealedThisMove = List.of(firstId, cardId);
            long reactionTimeMillis = reactionTimeMillis(firstFlipAt, Instant.now());

            if (firstCard.getSymbolKey().equals(card.getSymbolKey())) {
                // ✅ Match
                firstCard.setMatched(true);
                card.setMatched(true);
                session.setCurrentConsecutiveMatchStreak(
                    session.getCurrentConsecutiveMatchStreak() + 1);
                session.setMaxConsecutiveMatchStreak(Math.max(
                    session.getMaxConsecutiveMatchStreak(),
                    session.getCurrentConsecutiveMatchStreak()));
                recordCompletedMove(session, board.size() / 2, reactionTimeMillis);
                // isFlipped stays true (face-up permanently)
                flipResult = "MATCH";

                if (board.stream().allMatch(CardState::isMatched)) {
                    // 🎉 Win condition — all pairs found
                    wonGame = true;
                    long elapsed = computeElapsedSeconds(session);
                    int finalScore = calculateScore(
                            session.getMoves(), elapsed,
                            session.getDifficulty(), session.getHintsUsed());
                    session.setScore(finalScore);
                    session.setStatus(GameStatus.WON);
                    updatePerformanceMetrics(session, board.size() / 2, elapsed);
                    writeScoreRecord(session, elapsed);
                    writePerformanceHistory(session, board.size() / 2, elapsed);
                }
            } else {
                // ❌ No match — flip both cards back
                firstCard.setFlipped(false);
                card.setFlipped(false);
                session.setMistakeCount(session.getMistakeCount() + 1);
                session.setCurrentConsecutiveMatchStreak(0);
                recordCompletedMove(session, board.size() / 2, reactionTimeMillis);
                flipResult = "NO_MATCH";
            }

            session.setFirstFlippedCardId(-1);
            session.setFirstFlipAt(null);
            session.setMoveDeadlineAt(null);
        }

        session.setBoardStateJson(toJson(board));
        sessionRepository.save(session);

        GameSessionResponse resp = buildResponse(session, board);
        resp.setFlipResult(flipResult);
        resp.setWonGame(wonGame);
        resp.setRevealedThisMove(revealedThisMove);

        // Force-reveal the symbol for the card(s) involved in this move, even
        // if their flipped/matched flags say hidden (true for NO_MATCH, where
        // both cards were already flipped back down above). Without this the
        // client has no way to show what the second card actually was before
        // it flips back — it would just look like the second card never flipped.
        Map<Integer, String> trueSymbols = board.stream()
                .collect(Collectors.toMap(CardState::getCardId, CardState::getSymbolKey));
        resp.getBoard().stream()
                .filter(dto -> revealedThisMove.contains(dto.getCardId()))
                .forEach(dto -> dto.setSymbolKey(trueSymbols.get(dto.getCardId())));

        resp.setMessage(flipMessage(flipResult, wonGame, session.getScore()));
        return resp;
    }

    /**
     * Pauses the game and freezes the server-side timer.
     */
    @Transactional
    public GameSessionResponse pauseGame(Long sessionId) {
        GameSession session = findSession(sessionId);
        if (session.getStatus() != GameStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot pause — session is not ACTIVE (current: " + session.getStatus() + ").");
        }
        session.setStatus(GameStatus.PAUSED);
        session.setPausedAt(Instant.now());
        sessionRepository.save(session);

        GameSessionResponse resp = buildResponse(session, fromJson(session.getBoardStateJson()));
        resp.setMessage("Game paused. Timer frozen at " + resp.getElapsedSeconds() + "s.");
        return resp;
    }

    /**
     * Resumes the game. The pause duration is accumulated into totalPausedSeconds
     * so that elapsedSeconds remains accurate.
     */
    @Transactional
    public GameSessionResponse resumeGame(Long sessionId) {
        GameSession session = findSession(sessionId);
        if (session.getStatus() != GameStatus.PAUSED) {
            throw new IllegalStateException(
                    "Cannot resume — session is not PAUSED (current: " + session.getStatus() + ").");
        }
        if (session.getPausedAt() != null) {
            long pausedDuration = Duration.between(session.getPausedAt(), Instant.now()).getSeconds();
            session.setTotalPausedSeconds(session.getTotalPausedSeconds() + pausedDuration);
        }
        session.setPausedAt(null);
        session.setStatus(GameStatus.ACTIVE);
        sessionRepository.save(session);

        GameSessionResponse resp = buildResponse(session, fromJson(session.getBoardStateJson()));
        resp.setMessage("Game resumed. Timer running.");
        return resp;
    }

    /**
     * Re-shuffles the board under the same session (same difficulty and theme).
     * Resets all counters. Works even on WON/LOST sessions (play again).
     */
    @Transactional
    public GameSessionResponse restartGame(Long sessionId) {
        GameSession session = findSession(sessionId);
        List<CardState> freshBoard = generateBoard(session.getDifficulty(), session.getTheme());

        session.setBoardStateJson(toJson(freshBoard));
        session.setMoves(0);
        session.setHintsUsed(0);
        session.setScore(0);
        session.setAverageReactionTimeMillis(0L);
        session.setMistakeCount(0);
        session.setCurrentConsecutiveMatchStreak(0);
        session.setMaxConsecutiveMatchStreak(0);
        session.setConcentrationScore(100.0);
        session.setAccuracyPercent(0.0);
        Instant restartedAt = Instant.now();
        session.setPreviewEndsAt(restartedAt.plusSeconds(previewDurationSeconds));
        session.setFirstFlipAt(null);
        session.setMoveDeadlineAt(null);
        session.setMoveTimeLimitSeconds(initialMoveTimeLimit(
            session.getDifficulty(), session.isFocusMode()));
        session.setFirstFlippedCardId(-1);
        session.setStatus(GameStatus.ACTIVE);
        session.setStartedAt(Instant.now());
        session.setPausedAt(null);
        session.setTotalPausedSeconds(0L);

        sessionRepository.save(session);

        GameSessionResponse resp = buildResponse(session, freshBoard);
        resp.setMessage("Board reshuffled. Good luck!");
        return resp;
    }

    /**
     * Returns the card IDs of one unmatched pair (the "hint").
     * Increments hintsUsed; the 50-point penalty is applied to the final score at win time.
     */
    @Transactional
    public HintResponse getHint(Long sessionId) {
        GameSession session = requireActive(sessionId);
        List<CardState> board = fromJson(session.getBoardStateJson());

        // Group all still-unmatched cards by symbol key
        Map<String, List<CardState>> unmatchedGroups = board.stream()
                .filter(c -> !c.isMatched())
                .collect(Collectors.groupingBy(CardState::getSymbolKey));

        Optional<Map.Entry<String, List<CardState>>> pairEntry = unmatchedGroups.entrySet()
                .stream()
                .filter(e -> e.getValue().size() == 2)
                .findFirst();

        if (pairEntry.isEmpty()) {
            throw new IllegalStateException(
                    "No unmatched pair found — all cards should already be matched!");
        }

        List<Integer> hintIds = pairEntry.get().getValue().stream()
                .map(CardState::getCardId)
                .collect(Collectors.toList());

        session.setHintsUsed(session.getHintsUsed() + 1);
        sessionRepository.save(session);

        return new HintResponse(hintIds, session.getHintsUsed(), HINT_PENALTY);
    }

    /**
     * Explicit save endpoint. State is also auto-saved on every other action,
     * so this mainly exists to allow the frontend to call it on page-unload.
     */
    @Transactional
    public GameSessionResponse saveProgress(Long sessionId) {
        GameSession session = findSession(sessionId);
        // @PreUpdate will fire on save() and refresh updatedAt
        sessionRepository.save(session);
        GameSessionResponse resp = buildResponse(session, fromJson(session.getBoardStateJson()));
        resp.setMessage("Progress saved at " + java.time.LocalDateTime.now() + ".");
        return resp;
    }

    /**
     * Returns the most recent ACTIVE or PAUSED session for a user so they can
     * continue where they left off.
     */
    @Transactional(readOnly = true)
    public GameSessionResponse loadGame(Long userId) {
        findUser(userId); // validate user exists first
        GameSession session = sessionRepository
                .findTopByUserIdAndStatusInOrderByUpdatedAtDesc(
                        userId, Arrays.asList(GameStatus.ACTIVE, GameStatus.PAUSED))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active or paused game found for user " + userId + ". Start a new game first."));

        GameSessionResponse resp = buildResponse(session, fromJson(session.getBoardStateJson()));
        resp.setMessage("Loaded saved game (status: " + session.getStatus()
                + ", elapsed: " + resp.getElapsedSeconds() + "s).");
        return resp;
    }

    /**
     * Fetches any session by ID (useful for polling state without doing an action).
     */
    @Transactional(readOnly = true)
    public GameSessionResponse getSession(Long sessionId) {
        GameSession session = findSession(sessionId);
        return buildResponse(session, fromJson(session.getBoardStateJson()));
    }

    /**
     * Score formula:
     *   score = max(0, base − moves×2 − hintsUsed×50 − elapsedSeconds×1)
     *
     * Base scores: EASY=500, MEDIUM=1000, HARD=2000
     */
    public int calculateScore(int moves, long timeTaken, Difficulty difficulty, int hintsUsed) {
        int base    = BASE_SCORE.getOrDefault(difficulty, 500);
        int penalty = moves * MOVES_PENALTY
                    + hintsUsed * HINT_PENALTY
                    + (int) Math.min(timeTaken, Integer.MAX_VALUE) * TIME_PENALTY;
        return Math.max(0, base - penalty);
    }

    /**
     * Returns score history for a user, newest first.
     * Called by ScoreController.
     */
    @Transactional(readOnly = true)
    public List<ScoreRecordResponse> getScoreHistory(Long userId) {
        findUser(userId);
        return scoreRepository.findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .map(this::toScoreResponse)
                .collect(Collectors.toList());
    }

    /** Returns completed cognitive-performance snapshots for a player, newest first. */
    @Transactional(readOnly = true)
    public List<PerformanceHistory> getPerformanceHistory(Long userId) {
        findUser(userId);
        return performanceHistoryRepository.findByPlayerIdOrderBySessionDateDesc(userId);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Builds a shuffled board: pick the first `pairsCount` symbols from the theme,
     * duplicate them to form pairs, shuffle, then assign sequential card IDs.
     */
    private List<CardState> generateBoard(Difficulty difficulty, String theme) {
        int pairsCount = PAIRS.get(difficulty);
        List<String> pool = THEME_SYMBOLS.getOrDefault(theme, THEME_SYMBOLS.get("animals"));

        if (pool.size() < pairsCount) {
            throw new IllegalStateException(
                    "Theme '" + theme + "' has only " + pool.size()
                            + " symbols but " + pairsCount + " pairs are needed.");
        }

        // Take first `pairsCount` unique symbols and duplicate into pairs
        List<String> symbols = new ArrayList<>(pool.subList(0, pairsCount));
        symbols.addAll(new ArrayList<>(pool.subList(0, pairsCount)));
        Collections.shuffle(symbols); // java.util.Collections

        List<CardState> board = new ArrayList<>(symbols.size());
        for (int i = 0; i < symbols.size(); i++) {
            board.add(new CardState(i, symbols.get(i)));
        }
        return board;
    }

    /** Resolves the requested theme to a valid key, falling back to "animals". */
    private String resolveTheme(String theme) {
        if (theme == null || theme.isBlank()) return "animals";
        String key = theme.trim().toLowerCase();
        return THEME_SYMBOLS.containsKey(key) ? key : "animals";
    }

    /**
     * Computes elapsed active-play seconds without trusting the client.
     *   elapsed = (now or pausedAt) − startedAt − totalPausedSeconds
     */
    private long computeElapsedSeconds(GameSession session) {
        if (session.getStartedAt() == null) return 0L;
        Instant reference = (session.getStatus() == GameStatus.PAUSED
                             && session.getPausedAt() != null)
                            ? session.getPausedAt()
                            : Instant.now();
        long raw = Duration.between(session.getStartedAt(), reference).getSeconds();
        return Math.max(0L, raw - session.getTotalPausedSeconds());
    }

    /** Writes a ScoreRecord when a session is won. */
    private void writeScoreRecord(GameSession session, long elapsedSeconds) {
        ScoreRecord record = new ScoreRecord(
                session.getUser(),
                session.getDifficulty(),
                session.getMoves(),
                elapsedSeconds,
                session.getScore(),
                session.getHintsUsed()
        );
        scoreRepository.save(record);
    }

    private void updatePerformanceMetrics(GameSession session, int totalPairs, long elapsedSeconds) {
        session.setConcentrationScore(Math.max(0.0,
            Math.min(100.0, session.getAccuracyPercent() - (elapsedSeconds * 0.1))));
    }

    private void writePerformanceHistory(GameSession session, int totalPairs, long elapsedSeconds) {
        int moves = session.getMoves();
        double accuracy = moves == 0 ? 0.0 : (totalPairs * 100.0) / moves;
        PerformanceHistory history = new PerformanceHistory(
                session.getUser(),
                java.time.LocalDateTime.now(),
                session.getScore(),
                accuracy,
                elapsedSeconds,
                session.getMistakeCount(),
                session.getMaxConsecutiveMatchStreak());
        performanceHistoryRepository.save(history);
    }

    private int initialMoveTimeLimit(Difficulty difficulty, boolean focusMode) {
        Map<Difficulty, Integer> limits = focusMode
                ? FOCUS_MOVE_LIMIT_SECONDS : BASE_MOVE_LIMIT_SECONDS;
        return limits.getOrDefault(difficulty, 10);
    }

    private boolean isPreviewActive(GameSession session) {
        return session.getPreviewEndsAt() != null
                && Instant.now().isBefore(session.getPreviewEndsAt());
    }

    private boolean isMoveExpired(GameSession session) {
        return session.getMoveDeadlineAt() != null
                && !Instant.now().isBefore(session.getMoveDeadlineAt());
    }

    private void expireTimedOutMove(GameSession session, List<CardState> board, CardState firstCard) {
        firstCard.setFlipped(false);
        session.setBoardStateJson(toJson(board));
        session.setFirstFlippedCardId(-1);
        session.setFirstFlipAt(null);
        session.setMoveDeadlineAt(null);
        session.setMistakeCount(session.getMistakeCount() + 1);
        session.setCurrentConsecutiveMatchStreak(0);
        sessionRepository.save(session);
    }

    private long reactionTimeMillis(Instant firstFlipAt, Instant secondFlipAt) {
        if (firstFlipAt == null) return 0L;
        return Math.max(0L, Duration.between(firstFlipAt, secondFlipAt).toMillis());
    }

    private void recordCompletedMove(GameSession session, int totalPairs, long reactionTimeMillis) {
        int moves = session.getMoves();
        long previousAverage = session.getAverageReactionTimeMillis();
        session.setAverageReactionTimeMillis(
                moves == 0 ? reactionTimeMillis
                        : ((previousAverage * (moves - 1L)) + reactionTimeMillis) / moves);
        session.setAccuracyPercent(moves == 0 ? 0.0
                : (session.getMoves() - session.getMistakeCount()) * 100.0 / moves);
        updateAdaptiveDifficulty(session);
    }

    private void updateAdaptiveDifficulty(GameSession session) {
        if (session.getMoves() < 3) return;
        int limit = session.getMoveTimeLimitSeconds();
        long average = session.getAverageReactionTimeMillis();
        int baseLimit = initialMoveTimeLimit(session.getDifficulty(), session.isFocusMode());
        int maxLimit = baseLimit + (session.isFocusMode() ? 2 : 4);
        if (session.getAccuracyPercent() >= 75.0 && average <= limit * 600L) {
            session.setMoveTimeLimitSeconds(Math.max(3, limit - 1));
        } else if (session.getAccuracyPercent() < 50.0 || average > limit * 900L) {
            session.setMoveTimeLimitSeconds(Math.min(maxLimit, limit + 1));
        }
    }

    /** Maps a GameSession + board list to the standard response DTO. */
    private GameSessionResponse buildResponse(GameSession session, List<CardState> board) {
        GameSessionResponse resp = new GameSessionResponse();
        resp.setSessionId(session.getId());
        resp.setUserId(session.getUser().getId());
        resp.setDifficulty(session.getDifficulty().name());
        resp.setTheme(session.getTheme());
        resp.setMoves(session.getMoves());
        resp.setElapsedSeconds(computeElapsedSeconds(session));
        resp.setStatus(session.getStatus().name());
        resp.setHintsUsed(session.getHintsUsed());
        resp.setScore(session.getScore());
        resp.setAccuracyPercent(session.getAccuracyPercent());
        resp.setCurrentStreak(session.getCurrentConsecutiveMatchStreak());
        resp.setMaxStreak(session.getMaxConsecutiveMatchStreak());
        resp.setAverageReactionTimeMillis(session.getAverageReactionTimeMillis());
        resp.setConcentrationScore(session.getConcentrationScore());
        resp.setPreviewing(isPreviewActive(session));
        resp.setPreviewEndsAt(session.getPreviewEndsAt());
        resp.setFocusMode(session.isFocusMode());
        resp.setMoveTimeLimitSeconds(session.getMoveTimeLimitSeconds());
        resp.setMoveDeadlineAt(session.getMoveDeadlineAt());
        boolean revealPreview = resp.isPreviewing();
        resp.setBoard(board.stream()
            .map(card -> toCardDto(card, revealPreview))
            .collect(Collectors.toList()));
        return resp;
    }

    /**
     * Converts a CardState to its DTO, masking the symbolKey if the card is
     * face-down and not yet matched (i.e., hidden from the player).
     */
    private CardDto toCardDto(CardState c, boolean revealAll) {
        String symbol = (revealAll || c.isFlipped() || c.isMatched()) ? c.getSymbolKey() : null;
        return new CardDto(c.getCardId(), symbol, c.isFlipped(), c.isMatched());
    }

    private ScoreRecordResponse toScoreResponse(ScoreRecord r) {
        ScoreRecordResponse resp = new ScoreRecordResponse();
        resp.setId(r.getId());
        resp.setUserId(r.getUser().getId());
        resp.setUsername(r.getUser().getUsername());
        resp.setDifficulty(r.getDifficulty().name());
        resp.setMoves(r.getMoves());
        resp.setTimeTakenSeconds(r.getTimeTakenSeconds());
        resp.setScore(r.getScore());
        resp.setHintsUsed(r.getHintsUsed());
        resp.setPlayedAt(r.getPlayedAt());
        return resp;
    }

    /** Looks up a card in the board by ID. Throws if not found. */
    private CardState requireCard(List<CardState> board, int cardId) {
        return board.stream()
                .filter(c -> c.getCardId() == cardId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Card ID " + cardId + " does not exist on this board."));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private GameSession findSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    /** findSession + status guard: must be ACTIVE. */
    private GameSession requireActive(Long sessionId) {
        GameSession s = findSession(sessionId);
        if (s.getStatus() != GameStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Session " + sessionId + " is not ACTIVE (current: " + s.getStatus()
                            + "). Resume the game first.");
        }
        return s;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize board state to JSON.", e);
        }
    }

    private List<CardState> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<CardState>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize board state from JSON.", e);
        }
    }

    private String flipMessage(String flipResult, boolean wonGame, int finalScore) {
        if (wonGame) {
            return "🎉 Congratulations! You matched all pairs! Final score: " + finalScore + ".";
        }
        return switch (flipResult) {
            case "FIRST_FLIP" -> "Card flipped. Now pick a second card to try to match it.";
            case "MATCH"      -> "✅ Match found! Keep going.";
            case "NO_MATCH"   -> "❌ No match — both cards flipped back. Try again!";
            default           -> "";
        };
    }
}
