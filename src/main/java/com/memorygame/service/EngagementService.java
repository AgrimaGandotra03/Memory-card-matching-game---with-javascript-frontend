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

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long userId) {
        findUser(userId);
        List<PerformanceHistory> recent = historyRepository.findByPlayerIdOrderBySessionDateDesc(userId)
                .stream().limit(10).collect(Collectors.toList());
        List<RecommendationResponse> recommendations = new ArrayList<>();
        if (recent.isEmpty()) {
            recommendations.add(new RecommendationResponse("Build your baseline", "Complete a few sessions so your training trends can be measured.", "CLASSIC"));
            return recommendations;
        }
        double accuracy = recent.stream().mapToDouble(PerformanceHistory::getAccuracyPercent).average().orElse(0);
        double reaction = recent.stream().mapToDouble(h -> h.getTotalTimeSeconds()).average().orElse(0);
        if (accuracy < 70) {
            recommendations.add(new RecommendationResponse("Sharpen accuracy", "Your recent accuracy is below 70%. Slow down and use Focus Mode to reduce mistakes.", "CLASSIC"));
        }
        if (reaction > 90) {
            recommendations.add(new RecommendationResponse("Train speed", "Your recent sessions are taking over 90 seconds. Try Timed Challenge for shorter decisions.", "TIMED_CHALLENGE"));
        }
        if (recommendations.isEmpty()) {
            recommendations.add(new RecommendationResponse("Raise the challenge", "Your recent accuracy is strong. Progressive mode can stretch your memory further.", "PROGRESSIVE"));
        }
        if (recent.size() >= 4) {
            double newest = recent.get(0).getAccuracyPercent();
            double oldest = recent.get(recent.size() - 1).getAccuracyPercent();
            if (newest > oldest * 1.2) {
                recommendations.add(new RecommendationResponse("Great momentum", "Your accuracy improved by more than 20% across recent sessions. Try Timed Challenge.", "TIMED_CHALLENGE"));
            }
        }
        return recommendations.stream().limit(2).collect(Collectors.toList());
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
