package com.memorygame.service;

import com.memorygame.dto.*;
import com.memorygame.model.*;
import com.memorygame.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EngagementService {
    private static final List<String> SYMBOLS = List.of(
            "🐶", "🐱", "🦊", "🐼", "🦁", "🐸", "🐙", "🦋", "🦄", "🐝", "🐬", "🚀", "🌙", "⭐", "🌍", "⚡");
    private static final Map<String, String[]> BADGES = Map.of(
            "TEN_STREAK", new String[]{"10-Streak", "Complete a session with a 10-match streak."},
            "SUB_TWO_MINUTES", new String[]{"Speed Clear", "Complete a session in under two minutes."},
            "DAILY_SEVEN", new String[]{"Daily Devotee", "Complete daily challenges on seven consecutive days."}
    );

    private final UserRepository userRepository;
    private final PerformanceHistoryRepository historyRepository;
    private final DailyChallengeCompletionRepository dailyRepository;
    private final PlayerBadgeRepository badgeRepository;

    public EngagementService(UserRepository userRepository,
                             PerformanceHistoryRepository historyRepository,
                             DailyChallengeCompletionRepository dailyRepository,
                             PlayerBadgeRepository badgeRepository) {
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.dailyRepository = dailyRepository;
        this.badgeRepository = badgeRepository;
    }

    @Transactional(readOnly = true)
    public DailyChallengeResponse getDailyChallenge(Long userId, LocalDate date) {
        User user = findUser(userId);
        LocalDate challengeDate = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        long seed = challengeDate.toString().hashCode() * 31L + 20260913L;
        List<CardDto> board = dailyBoard(seed);
        DailyChallengeResponse response = new DailyChallengeResponse();
        response.setChallengeDate(challengeDate);
        response.setSeed(seed);
        dailyRepository.findByPlayerIdAndChallengeDate(user.getId(), challengeDate).ifPresent(completion -> {
            response.setCompleted(true);
            response.setCompletionScore(completion.getScore());
        });
        response.setBoard(board);
        return response;
    }

    @Transactional
    public DailyChallengeResponse completeDailyChallenge(Long userId, LocalDate date, int score) {
        User user = findUser(userId);
        LocalDate challengeDate = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        if (dailyRepository.findByPlayerIdAndChallengeDate(userId, challengeDate).isEmpty()) {
            dailyRepository.save(new DailyChallengeCompletion(user, challengeDate, score));
            evaluateBadges(userId);
        }
        return getDailyChallenge(userId, challengeDate);
    }

    @Transactional(readOnly = true)
    public List<BadgeResponse> getBadges(Long userId) {
        findUser(userId);
        return badgeRepository.findByPlayerIdOrderByEarnedAtDesc(userId).stream()
                .map(badge -> {
                    String[] definition = BADGES.getOrDefault(badge.getBadgeCode(),
                            new String[]{badge.getBadgeCode(), "Achievement earned."});
                    return new BadgeResponse(badge.getBadgeCode(), definition[0], definition[1], badge.getEarnedAt());
                }).collect(Collectors.toList());
    }

    private static final Map<String, Double> EXPECTED_SECONDS_BY_DIFFICULTY = Map.of(
            "EASY", 60.0, "MEDIUM", 150.0, "HARD", 300.0);
    private static final List<String> ALL_MODES = List.of(
            "CLASSIC", "TIMED_CHALLENGE", "SEQUENCE_MEMORY", "PROGRESSIVE");
    private static final Map<String, String> MODE_PITCH = Map.of(
            "CLASSIC", "the classic pair-matching format",
            "TIMED_CHALLENGE", "matching every pair against a countdown",
            "SEQUENCE_MEMORY", "watching and repeating a card order",
            "PROGRESSIVE", "three escalating levels in one run");

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long userId) {
        findUser(userId);
        List<PerformanceHistory> all = historyRepository.findByPlayerIdOrderBySessionDateDesc(userId);
        if (all.isEmpty()) {
            return List.of(new RecommendationResponse("Build your baseline",
                    "Complete a few sessions so your training trends can be measured.", "CLASSIC"));
        }
        List<PerformanceHistory> recent = all.stream().limit(10).collect(Collectors.toList());

        double avgAccuracy = recent.stream().mapToDouble(PerformanceHistory::getAccuracyPercent).average().orElse(0);
        double avgMistakes = recent.stream().mapToDouble(PerformanceHistory::getMistakeCount).average().orElse(0);
        double avgStreak = recent.stream().mapToDouble(PerformanceHistory::getMaxStreak).average().orElse(0);
        double avgConcentration = recent.stream()
                .mapToDouble(PerformanceHistory::getConcentrationScore).average().orElse(100);
        // Compares actual time against a difficulty-scaled expectation, since a flat
        // "over 90 seconds" threshold doesn't mean the same thing on an 8-pair Easy
        // board as it does on a 32-pair Hard board.
        double avgTimeRatio = recent.stream()
                .mapToDouble(h -> h.getTotalTimeSeconds()
                        / EXPECTED_SECONDS_BY_DIFFICULTY.getOrDefault(
                                h.getDifficulty() == null ? "MEDIUM" : h.getDifficulty(), 90.0))
                .average().orElse(1.0);

        List<Object[]> candidates = new ArrayList<>(); // [priority(Integer), RecommendationResponse]

        if (avgConcentration < 55) {
            candidates.add(new Object[]{100, new RecommendationResponse("Improve focus",
                    "Your concentration score has been running low. Try Focus Mode to cut down on-screen distractions and steady your pace.",
                    "CLASSIC")});
        }
        if (avgAccuracy < 70) {
            candidates.add(new Object[]{90, new RecommendationResponse("Sharpen accuracy",
                    String.format("Your recent accuracy is averaging %.0f%%. Slow down and use Focus Mode to reduce mistakes.", avgAccuracy),
                    "CLASSIC")});
        } else if (avgMistakes > 6) {
            candidates.add(new Object[]{80, new RecommendationResponse("Cut down mistakes",
                    String.format("You're averaging %.0f mistakes per session even with solid accuracy — try using a hint earlier instead of guessing.", avgMistakes),
                    "CLASSIC")});
        }
        if (avgTimeRatio > 1.2) {
            candidates.add(new Object[]{75, new RecommendationResponse("Train speed",
                    "Your sessions are taking noticeably longer than expected for their difficulty. Try Timed Challenge for shorter, decisive moves.",
                    "TIMED_CHALLENGE")});
        }
        if (avgAccuracy >= 70 && avgStreak < 3) {
            candidates.add(new Object[]{65, new RecommendationResponse("Build streaks",
                    "Your accuracy is solid but your match streaks stay short. Focus on remembering pairs you've already seen instead of re-flipping at random.",
                    "CLASSIC")});
        }
        if (recent.size() >= 4) {
            int half = recent.size() / 2;
            double newerAvg = recent.subList(0, half).stream()
                    .mapToDouble(PerformanceHistory::getAccuracyPercent).average().orElse(0);
            double olderAvg = recent.subList(half, recent.size()).stream()
                    .mapToDouble(PerformanceHistory::getAccuracyPercent).average().orElse(0);
            if (olderAvg > 0 && newerAvg > olderAvg * 1.15) {
                candidates.add(new Object[]{60, new RecommendationResponse("Great momentum",
                        String.format("Your accuracy has climbed from an average of %.0f%% to %.0f%% across your recent sessions. Try Timed Challenge to put that improvement to the test.", olderAvg, newerAvg),
                        "TIMED_CHALLENGE")});
            }
        }
        Set<String> modesPlayed = all.stream()
                .map(PerformanceHistory::getMode).filter(Objects::nonNull).collect(Collectors.toSet());
        for (String candidateMode : ALL_MODES) {
            if (!modesPlayed.contains(candidateMode)) {
                candidates.add(new Object[]{50, new RecommendationResponse("Try something new",
                        "You haven't played " + candidateMode.replace('_', ' ').toLowerCase()
                                + " yet — it's " + MODE_PITCH.getOrDefault(candidateMode, "a different way to train") + ".",
                        candidateMode)});
                break; // only surface one untried mode at a time
            }
        }
        if (candidates.isEmpty()) {
            candidates.add(new Object[]{10, new RecommendationResponse("Raise the challenge",
                    "Your recent accuracy and pace are strong. Progressive mode can stretch your memory further.",
                    "PROGRESSIVE")});
        }

        return candidates.stream()
                .sorted((a, b) -> (Integer) b[0] - (Integer) a[0])
                .map(c -> (RecommendationResponse) c[1])
                .limit(3)
                .collect(Collectors.toList());
    }

    @Transactional
    public void evaluateBadges(Long userId) {
        User user = findUser(userId);
        List<PerformanceHistory> history = historyRepository.findByPlayerIdOrderBySessionDateDesc(userId);
        if (history.stream().anyMatch(h -> h.getMaxStreak() >= 10)) earn(user, "TEN_STREAK");
        if (history.stream().anyMatch(h -> h.getTotalTimeSeconds() < 120)) earn(user, "SUB_TWO_MINUTES");
        List<DailyChallengeCompletion> daily = dailyRepository.findByPlayerIdOrderByChallengeDateDesc(userId);
        if (hasSevenDayStreak(daily)) earn(user, "DAILY_SEVEN");
    }

    private boolean hasSevenDayStreak(List<DailyChallengeCompletion> completions) {
        Set<LocalDate> dates = completions.stream().map(DailyChallengeCompletion::getChallengeDate).collect(Collectors.toSet());
        for (LocalDate date : dates) {
            boolean streak = true;
            for (int offset = 1; offset < 7; offset++) if (!dates.contains(date.minusDays(offset))) streak = false;
            if (streak) return true;
        }
        return false;
    }

    private void earn(User user, String code) {
        if (!badgeRepository.existsByPlayerIdAndBadgeCode(user.getId(), code)) badgeRepository.save(new PlayerBadge(user, code));
    }

    private List<CardDto> dailyBoard(long seed) {
        List<String> symbols = new ArrayList<>(SYMBOLS.subList(0, 8));
        symbols.addAll(new ArrayList<>(symbols));
        Collections.shuffle(symbols, new Random(seed));
        List<CardDto> board = new ArrayList<>();
        for (int i = 0; i < symbols.size(); i++) board.add(new CardDto(i, symbols.get(i), false, false));
        return board;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
}
