package com.ayago.smartuitest.testscenario.parser;

import com.ayago.smartuitest.testscenario.Action;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ActionFactory {
    private final Map<ActionType, ActionFactoryStrategy> strategies;
    
    public ActionFactory(List<ActionFactoryStrategy> strategies) {
        this.strategies = strategies.stream()
            .collect(Collectors.toMap(ActionFactoryStrategy::forActionType, Function.identity()));
    }
    
    public Action getAction(ActionType type, Map<String, String> data) {
        ActionFactoryStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown action type: " + type);
        }
        return strategy.createAction(data);
    }
}
