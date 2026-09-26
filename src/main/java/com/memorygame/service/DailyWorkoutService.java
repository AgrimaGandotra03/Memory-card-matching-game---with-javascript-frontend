package com.memorygame.service;

import com.memorygame.dto.*;
import com.memorygame.model.*;
import com.memorygame.repository.PlayerBadgeRepository;
import com.memorygame.repository.UserRepository;
import com.memorygame.repository.WorkoutAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Drives the "Daily Brain Workout" feature: a 5-day rotating set of short cognitive-training
 * mini-games layered on top of the existing memory-card game. It reuses the existing User
 * entity for identity and PlayerBadgeRepository for achievement badges, and adds only one new
 * table (WorkoutAttempt) rather than a parallel user system.
 *
 * Like the existing daily-challenge feature in EngagementService, this trusts the frontend to
 * report its own play metrics (correct/incorrect counts, timing) rather than replaying every
 * click server-side — consistent with the rest of this prototype's trust model.
 */
@Service
public class DailyWorkoutService {

    private static final List<String> SYMBOLS = List.of(
            "🐶", "🐱", "🦊", "🐼", "🦁", "🐸", "🐙", "🦋", "🦄", "🐝",
            "🐬", "🚀", "🌙", "⭐", "🌍", "⚡", "🐷", "🐵", "🐧", "🦉");

    private static final Map<DayOfWeek, WorkoutSkill> ROTATION = Map.of(
            DayOfWeek.MONDAY, WorkoutSkill.WORKING_MEMORY,
            DayOfWeek.TUESDAY, WorkoutSkill.ATTENTION,
            DayOfWeek.WEDNESDAY, WorkoutSkill.PROCESSING_SPEED,
            DayOfWeek.THURSDAY, WorkoutSkill.SEQUENCE_MEMORY,
            DayOfWeek.FRIDAY, WorkoutSkill.DELAYED_RECALL);

    private static final Map<WorkoutSkill, String> SKILL_LABELS = Map.of(
            WorkoutSkill.WORKING_MEMORY, "Working Memory",
            WorkoutSkill.ATTENTION, "Attention",
            WorkoutSkill.PROCESSING_SPEED, "Processing Speed",
            WorkoutSkill.SEQUENCE_MEMORY, "Sequence Memory",
            WorkoutSkill.DELAYED_RECALL, "Delayed Recall");

    private static final Map<WorkoutSkill, String> SKILL_DESCRIPTIONS = Map.of(
            WorkoutSkill.WORKING_MEMORY, "Remember where each card is hiding, then recall its exact spot once the board goes blank.",
            WorkoutSkill.ATTENTION, "Find every matching pair while decoy cards try to pull your focus away.",
            WorkoutSkill.PROCESSING_SPEED, "Match as many pairs as you can before the 60-second clock runs out — accuracy still counts.",
            WorkoutSkill.SEQUENCE_MEMORY, "Watch the cards light up in order, then repeat that exact sequence from memory.",
            WorkoutSkill.DELAYED_RECALL, "Study the cards, do a short unrelated activity, then recall what you saw after the delay.");

    private static final Map<WorkoutSkill, Integer> ESTIMATED_MINUTES = Map.of(
            WorkoutSkill.WORKING_MEMORY, 5,
            WorkoutSkill.ATTENTION, 5,
            WorkoutSkill.PROCESSING_SPEED, 3,
            WorkoutSkill.SEQUENCE_MEMORY, 4,
            WorkoutSkill.DELAYED_RECALL, 6);

    public static final String WORKOUT_STREAK_BADGE = "WORKOUT_STREAK_5";

    private final UserRepository userRepository;
    private final WorkoutAttemptRepository attemptRepository;
    private final PlayerBadgeRepository badgeRepository;

    public DailyWorkoutService(UserRepository userRepository,
                                WorkoutAttemptRepository attemptRepository,
                                PlayerBadgeRepository badgeRepository) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.badgeRepository = badgeRepository;
    }

    // ── Today's workout ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DailyWorkoutResponse getTodayWorkout(Long userId, LocalDate date) {
        User user = findUser(userId);
        LocalDate today = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        WorkoutSkill skill = skillForDate(today);
        boolean weekendBonus = !ROTATION.containsKey(today.getDayOfWeek());

        WorkoutLevel level = currentLevel(userId, skill);
        List<WorkoutAttempt> todaysAttempts =
                attemptRepository.findByPlayerIdAndAttemptDateAndSkillOrderByCreatedAtDesc(userId, today, skill);

        DailyWorkoutResponse response = new DailyWorkoutResponse();
        response.setDate(today);
        response.setSkill(skill.name());
        response.setSkillLabel(SKILL_LABELS.get(skill));
        response.setDescription(SKILL_DESCRIPTIONS.get(skill));
        response.setLevel(level.name());
        response.setLevelStars(level.ordinal() + 1);
        response.setEstimatedMinutes(ESTIMATED_MINUTES.get(skill));
        response.setWeekendBonus(weekendBonus);
        response.setCompletedToday(!todaysAttempts.isEmpty());
        response.setTodayScore(todaysAttempts.isEmpty() ? null
                : todaysAttempts.stream().mapToInt(WorkoutAttempt::getScore).max().orElse(0));
        response.setPersonalBest(personalBest(userId, skill));
        response.setCurrentStreakDays(currentStreak(userId));

        long seed = (today + "|" + userId + "|" + skill).hashCode();
        response.setChallenge(generateChallenge(skill, level, seed));
        return response;
    }

    private WorkoutSkill skillForDate(LocalDate date) {
        WorkoutSkill weekday = ROTATION.get(date.getDayOfWeek());
        if (weekday != null) return weekday;
        // Weekend: deterministic "mixed challenge" — same skill for every user on a given week,
        // rotating through the five skills week over week rather than a fixed calendar date.
        int weekOfYear = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        WorkoutSkill[] all = WorkoutSkill.values();
        return all[Math.floorMod(weekOfYear, all.length)];
    }

    // ── Difficulty progression ──────────────────────────────────────────────

    private WorkoutLevel currentLevel(Long userId, WorkoutSkill skill) {
        List<WorkoutAttempt> recent = attemptRepository.findByPlayerIdAndSkillOrderByCreatedAtDesc(userId, skill)
                .stream().limit(3).collect(Collectors.toList());
        if (recent.isEmpty()) return WorkoutLevel.BEGINNER;
        double avgAccuracy = recent.stream().mapToDouble(WorkoutAttempt::getAccuracyPercent).average().orElse(0);
        if (avgAccuracy >= 85) return WorkoutLevel.ADVANCED;
        if (avgAccuracy >= 65) return WorkoutLevel.INTERMEDIATE;
        return WorkoutLevel.BEGINNER;
    }

    // ── Challenge generation ────────────────────────────────────────────────

    private WorkoutChallengeDto generateChallenge(WorkoutSkill skill, WorkoutLevel level, long seed) {
        Random rng = new Random(seed);
        WorkoutChallengeDto dto = new WorkoutChallengeDto();
        dto.setSkill(skill.name());
        dto.setLevel(level.name());

        switch (skill) {
            case WORKING_MEMORY -> buildWorkingMemory(dto, level, rng);
            case ATTENTION -> buildAttention(dto, level, rng);
            case PROCESSING_SPEED -> buildProcessingSpeed(dto, level, rng);
            case SEQUENCE_MEMORY -> buildSequenceMemory(dto, level, rng);
            case DELAYED_RECALL -> buildDelayedRecall(dto, level, rng);
        }
        return dto;
    }

    private void buildWorkingMemory(WorkoutChallengeDto dto, WorkoutLevel level, Random rng) {
        int itemCount = switch (level) { case BEGINNER -> 4; case INTERMEDIATE -> 6; case ADVANCED -> 8; };
        int gridCells = switch (level) { case BEGINNER -> 6; case INTERMEDIATE -> 9; case ADVANCED -> 12; };
        int previewSeconds = switch (level) { case BEGINNER -> 6; case INTERMEDIATE -> 5; case ADVANCED -> 4; };

        dto.setGridCells(gridCells);
        dto.setPreviewSeconds(previewSeconds);
        dto.setItems(placeItems(itemCount, gridCells, rng));
    }

    private void buildAttention(WorkoutChallengeDto dto, WorkoutLevel level, Random rng) {
        int pairs = switch (level) { case BEGINNER -> 5; case INTERMEDIATE -> 8; case ADVANCED -> 12; };
        int decoys = switch (level) { case BEGINNER -> 2; case INTERMEDIATE -> 3; case ADVANCED -> 4; };
        dto.setCards(buildBoard(pairs, decoys, rng));
    }

    private void buildProcessingSpeed(WorkoutChallengeDto dto, WorkoutLevel level, Random rng) {
        int pairs = switch (level) { case BEGINNER -> 6; case INTERMEDIATE -> 10; case ADVANCED -> 14; };
        dto.setCards(buildBoard(pairs, 0, rng));
        dto.setTimeLimitSeconds(60);
    }

    private void buildSequenceMemory(WorkoutChallengeDto dto, WorkoutLevel level, Random rng) {
        int startLength = switch (level) { case BEGINNER -> 3; case INTERMEDIATE -> 5; case ADVANCED -> 7; };
        int tileCount = 9;
        List<String> symbols = new ArrayList<>(SYMBOLS.subList(0, tileCount));
        Collections.shuffle(symbols, rng);
        List<WorkoutCardEntry> tiles = new ArrayList<>();
        for (int i = 0; i < tileCount; i++) tiles.add(new WorkoutCardEntry(i, symbols.get(i), false));

        List<Integer> sequence = new ArrayList<>();
        for (int i = 0; i < startLength; i++) sequence.add(rng.nextInt(tileCount));

        dto.setSequenceTiles(tiles);
        dto.setSequence(sequence);
    }

    private void buildDelayedRecall(WorkoutChallengeDto dto, WorkoutLevel level, Random rng) {
        int itemCount = switch (level) { case BEGINNER -> 4; case INTERMEDIATE -> 6; case ADVANCED -> 8; };
        int gridCells = switch (level) { case BEGINNER -> 6; case INTERMEDIATE -> 9; case ADVANCED -> 12; };
        int delaySeconds = switch (level) { case BEGINNER -> 15; case INTERMEDIATE -> 20; case ADVANCED -> 25; };
        int questionCount = switch (level) { case BEGINNER -> 4; case INTERMEDIATE -> 5; case ADVANCED -> 6; };

        List<WorkoutItemDto> items = placeItems(itemCount, gridCells, rng);
        dto.setGridCells(gridCells);
        dto.setPreviewSeconds(6);
        dto.setDelaySeconds(delaySeconds);
        dto.setItems(items);
        dto.setQuestions(buildRecallQuestions(items, gridCells, questionCount, rng));
    }

    private List<WorkoutItemDto> placeItems(int itemCount, int gridCells, Random rng) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < gridCells; i++) positions.add(i);
        Collections.shuffle(positions, rng);

        List<String> symbols = new ArrayList<>(SYMBOLS);
        Collections.shuffle(symbols, rng);

        List<WorkoutItemDto> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(new WorkoutItemDto(positions.get(i), symbols.get(i)));
        }
        return items;
    }

    private List<WorkoutCardEntry> buildBoard(int pairs, int decoys, Random rng) {
        List<String> pairSymbols = new ArrayList<>(SYMBOLS.subList(0, Math.min(pairs, SYMBOLS.size())));
        List<String> board = new ArrayList<>();
        for (String symbol : pairSymbols) {
            board.add(symbol);
            board.add(symbol);
        }
        List<String> decoyPool = new ArrayList<>(SYMBOLS);
        Collections.shuffle(decoyPool, rng);
        Set<Integer> decoyIndices = new HashSet<>();
        List<WorkoutCardEntry> entries = new ArrayList<>();
        Collections.shuffle(board, rng);
        for (int i = 0; i < board.size(); i++) entries.add(new WorkoutCardEntry(i, board.get(i), false));
        for (int i = 0; i < decoys && i < decoyPool.size(); i++) {
            entries.add(new WorkoutCardEntry(entries.size(), decoyPool.get(i), true));
        }
        Collections.shuffle(entries, rng);
        for (int i = 0; i < entries.size(); i++) entries.get(i).setCardId(i);
        return entries;
    }

    private List<RecallQuestionDto> buildRecallQuestions(List<WorkoutItemDto> items, int gridCells,
                                                           int count, Random rng) {
        List<WorkoutItemDto> shuffledItems = new ArrayList<>(items);
        Collections.shuffle(shuffledItems, rng);
        List<RecallQuestionDto> questions = new ArrayList<>();

        for (int i = 0; i < count && i < shuffledItems.size() * 2; i++) {
            WorkoutItemDto item = shuffledItems.get(i % shuffledItems.size());
            boolean askPosition = rng.nextBoolean();
            if (askPosition) {
                // "Where was the 🐶 card?" — options are position labels.
                List<String> options = new ArrayList<>();
                options.add(String.valueOf(item.getPosition() + 1));
                while (options.size() < 4) {
                    int candidate = rng.nextInt(gridCells) + 1;
                    String candidateStr = String.valueOf(candidate);
                    if (!options.contains(candidateStr)) options.add(candidateStr);
                }
                Collections.shuffle(options, rng);
                questions.add(new RecallQuestionDto(
                        "Where was the " + item.getSymbol() + " card? (position)",
                        options, String.valueOf(item.getPosition() + 1)));
            } else {
                // "Which card was in position N?" — options are symbols.
                List<String> options = new ArrayList<>();
                options.add(item.getSymbol());
                List<String> pool = new ArrayList<>(SYMBOLS);
                Collections.shuffle(pool, rng);
                for (String candidate : pool) {
                    if (options.size() >= 4) break;
                    if (!options.contains(candidate)) options.add(candidate);
                }
                Collections.shuffle(options, rng);
                questions.add(new RecallQuestionDto(
                        "Which card was in position " + (item.getPosition() + 1) + "?",
                        options, item.getSymbol()));
            }
        }
        return questions;
    }

    // ── Submitting a result ─────────────────────────────────────────────────

    @Transactional
    public WorkoutAttemptResponse submitAttempt(Long userId, WorkoutSubmitRequest request) {
        User user = findUser(userId);
        WorkoutSkill skill = WorkoutSkill.valueOf(request.getSkill());
        WorkoutLevel level = request.getLevel() != null
                ? WorkoutLevel.valueOf(request.getLevel())
                : currentLevel(userId, skill);

        double accuracy = computeAccuracy(request, skill);
        int score = computeScore(request, skill, level, accuracy);

        Integer previousBest = personalBest(userId, skill);
        boolean isBest = previousBest == null || score > previousBest;

        WorkoutAttempt attempt = new WorkoutAttempt();
        attempt.setPlayer(user);
        attempt.setSkill(skill);
        attempt.setLevel(level);
        attempt.setAttemptDate(LocalDate.now(ZoneOffset.UTC));
        attempt.setCorrectCount(request.getCorrectCount());
        attempt.setIncorrectCount(request.getIncorrectCount());
        attempt.setAccuracyPercent(accuracy);
        attempt.setTimeTakenSeconds(request.getTimeTakenSeconds());
        attempt.setScore(score);
        attempt.setAvgReactionTimeMillis(request.getAvgReactionTimeMillis());
        attempt.setMaxSequenceLength(request.getSequenceLength());
        attempt.setImmediateAccuracyPercent(request.getImmediateAccuracyPercent());
        attempt.setDelayedAccuracyPercent(request.getDelayedAccuracyPercent());
        attempt.setRetries(request.getRetries());
        attempt.setPersonalBest(isBest);
        attemptRepository.save(attempt);

        int streak = currentStreak(userId);
        if (streak >= 5 && !badgeRepository.existsByPlayerIdAndBadgeCode(userId, WORKOUT_STREAK_BADGE)) {
            badgeRepository.save(new PlayerBadge(user, WORKOUT_STREAK_BADGE));
        }

        WorkoutAttemptResponse response = new WorkoutAttemptResponse();
        response.setScore(score);
        response.setAccuracyPercent(accuracy);
        response.setPersonalBest(isBest);
        response.setPreviousBest(previousBest);
        response.setCurrentStreakDays(streak);
        response.setMessage(isBest ? "New personal best for " + SKILL_LABELS.get(skill) + "!"
                : "Workout complete — keep training to beat your best.");
        return response;
    }

    private double computeAccuracy(WorkoutSubmitRequest request, WorkoutSkill skill) {
        if (skill == WorkoutSkill.DELAYED_RECALL && request.getDelayedAccuracyPercent() != null) {
            return clamp(request.getDelayedAccuracyPercent(), 0, 100);
        }
        int total = request.getTotalItems() > 0
                ? request.getTotalItems()
                : request.getCorrectCount() + request.getIncorrectCount();
        if (total <= 0) return 0;
        return clamp(request.getCorrectCount() * 100.0 / total, 0, 100);
    }

    private int computeScore(WorkoutSubmitRequest request, WorkoutSkill skill, WorkoutLevel level, double accuracy) {
        double speedComponent;
        double accuracyWeight = 0.7;
        double speedWeight = 0.3;

        switch (skill) {
            case PROCESSING_SPEED -> {
                int expectedPairs = switch (level) { case BEGINNER -> 6; case INTERMEDIATE -> 10; case ADVANCED -> 14; };
                int matched = request.getMatchesFound() != null ? request.getMatchesFound() : request.getCorrectCount();
                speedComponent = clamp(matched * 100.0 / expectedPairs, 0, 100);
            }
            case SEQUENCE_MEMORY -> {
                int achieved = request.getSequenceLength() != null ? request.getSequenceLength() : 0;
                speedComponent = clamp(achieved * 100.0 / 10.0, 0, 100);
            }
            case DELAYED_RECALL -> {
                accuracyWeight = 0.85;
                speedWeight = 0.15;
                int expectedSeconds = switch (level) { case BEGINNER -> 30; case INTERMEDIATE -> 40; case ADVANCED -> 50; };
                speedComponent = timeBasedSpeed(request.getTimeTakenSeconds(), expectedSeconds);
            }
            default -> {
                int expectedSeconds = expectedSecondsFor(skill, level);
                speedComponent = timeBasedSpeed(request.getTimeTakenSeconds(), expectedSeconds);
            }
        }
        double score = accuracy * accuracyWeight + speedComponent * speedWeight;
        return (int) Math.round(clamp(score, 0, 100));
    }

    private double timeBasedSpeed(int timeTakenSeconds, int expectedSeconds) {
        if (timeTakenSeconds <= 0) return 100;
        double ratio = expectedSeconds / (double) timeTakenSeconds; // >1 means faster than expected
        return clamp(ratio * 100.0, 0, 100);
    }

    private int expectedSecondsFor(WorkoutSkill skill, WorkoutLevel level) {
        int base = switch (skill) {
            case WORKING_MEMORY -> 45;
            case ATTENTION -> 60;
            default -> 60;
        };
        int levelBonus = switch (level) { case BEGINNER -> 15; case INTERMEDIATE -> 0; case ADVANCED -> -10; };
        return Math.max(15, base + levelBonus);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ── Progress & streaks ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WorkoutProgressResponse getProgress(Long userId) {
        findUser(userId);
        List<WorkoutAttempt> all = attemptRepository.findByPlayerIdOrderByCreatedAtDesc(userId);

        Map<String, Integer> bestBySkill = new LinkedHashMap<>();
        for (WorkoutSkill skill : WorkoutSkill.values()) {
            Integer best = personalBest(userId, skill);
            if (best != null) bestBySkill.put(skill.name(), best);
        }

        List<WorkoutHistoryEntryDto> history = all.stream()
                .limit(20)
                .map(a -> new WorkoutHistoryEntryDto(
                        a.getAttemptDate(), a.getSkill().name(), SKILL_LABELS.get(a.getSkill()),
                        a.getLevel().name(), a.getScore(), a.getAccuracyPercent(),
                        a.getTimeTakenSeconds(), a.isPersonalBest()))
                .collect(Collectors.toList());

        WorkoutProgressResponse response = new WorkoutProgressResponse();
        response.setTotalWorkoutsCompleted(all.size());
        response.setCurrentStreakDays(currentStreak(userId));
        response.setPersonalBestBySkill(bestBySkill);
        response.setRecentHistory(history);
        return response;
    }

    private Integer personalBest(Long userId, WorkoutSkill skill) {
        OptionalInt max = attemptRepository.findByPlayerIdAndSkillOrderByCreatedAtDesc(userId, skill).stream()
                .mapToInt(WorkoutAttempt::getScore)
                .max();
        return max.isPresent() ? max.getAsInt() : null;
    }

    private int currentStreak(Long userId) {
        Set<LocalDate> dates = attemptRepository.findByPlayerIdOrderByCreatedAtDesc(userId).stream()
                .map(WorkoutAttempt::getAttemptDate)
                .collect(Collectors.toSet());
        if (dates.isEmpty()) return 0;

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate cursor = dates.contains(today) ? today
                : dates.contains(today.minusDays(1)) ? today.minusDays(1)
                : null;
        if (cursor == null) return 0;

        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
}
