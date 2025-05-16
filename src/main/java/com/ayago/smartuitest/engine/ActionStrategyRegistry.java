package com.ayago.smartuitest.engine;

import com.ayago.smartuitest.testscenario.Action;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A registry for {@link ActionStrategy} beans.
 * It collects all ActionStrategy implementations from the Spring context
 * and provides a way to retrieve the appropriate strategy for a given Action type.
 */
@Component
public class ActionStrategyRegistry {

    private final Map<Class<? extends Action>, ActionStrategy> strategyMap;
    
    /**
     * Constructs the registry.
     *
     */
    ActionStrategyRegistry(Collection<ActionStrategy> strategies) {
        if (strategies.isEmpty()) {
            System.err.println("Warning: No ActionStrategy beans found in the application context. " +
                "Action execution will likely fail. Ensure strategies are annotated with @Component " +
                "and component scanning is configured for the 'com.ayago.action' package.");
            strategyMap = new HashMap<>();
        } else {
            strategyMap = strategies.stream()
                .collect(Collectors.toMap(ActionStrategy::getActionType, Function.identity(),
                    (existing, replacement) -> {
                        System.err.println("Warning: Duplicate strategy found for action type: " +
                            existing.getActionType().getName() +
                            ". Using existing strategy: " + existing.getClass().getName() +
                            ", ignoring duplicate: " + replacement.getClass().getName());
                        return existing;
                    }
                ));
            System.out.println("Initialized ActionStrategyRegistry with strategies for action types: " + strategyMap.keySet());
        }
    }
    
    /**
     * Retrieves the appropriate strategy for the given Action object.
     *
     * @param action The Action instance for which to find a strategy.
     * @return The ActionStrategy capable of handling the provided action.
     * @throws IllegalArgumentException if the action is null or no strategy is found for the action's type.
     * @throws IllegalStateException if the registry was not properly initialized (e.g., no strategies found).
     */
    public ActionStrategy getStrategy(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null when retrieving a strategy.");
        }

        if (strategyMap.isEmpty()) {
            System.err.println("ActionStrategyRegistry has no strategies loaded. Cannot find strategy for: " + action.getClass().getName());
            throw new IllegalStateException("ActionStrategyRegistry contains no strategies. Check Spring component scanning and strategy bean definitions.");
        }
        
        ActionStrategy strategy = strategyMap.get(action.getClass());
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for action type: " + action.getClass().getName() +
                ". Available strategies are registered for types: " + strategyMap.keySet());
        }
        return strategy;
    }
}

