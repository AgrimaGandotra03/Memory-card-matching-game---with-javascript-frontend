package com.memorygame.service.mode;

import com.memorygame.model.GameMode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class GameModeStrategyRegistry {

    private final Map<GameMode, GameModeStrategy> strategies = new EnumMap<>(GameMode.class);

    public GameModeStrategyRegistry(List<GameModeStrategy> strategies) {
        strategies.forEach(strategy -> this.strategies.put(strategy.mode(), strategy));
    }

    public GameModeStrategy forMode(GameMode mode) {
        return strategies.getOrDefault(mode, strategies.get(GameMode.CLASSIC));
    }
}