package com.ayago.smartuitest.engine;

import com.ayago.smartuitest.testscenario.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap; // Explicitly import HashMap
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

    private Map<Class<? extends Action>, ActionStrategy> strategyMap;
    
    /**
     * Constructs the registry.
     *
     */
    @Autowired
    ActionStrategyRegistry(Collection<ActionStrategy> strategies) {
        if (strategies.isEmpty()) {
            System.err.println("Warning: No ActionStrategy beans found in the application context. " +
                "Action execution will likely fail. Ensure strategies are annotated with @Component " +
                "and component scanning is configured for the 'com.ayago.action' package.");
            // Initialize with an empty map to prevent NullPointerExceptions later,
            // though getStrategy will still fail to find strategies.
            strategyMap = new HashMap<>();
            // Depending on requirements, you might throw an IllegalStateException here:
            // throw new IllegalStateException("No ActionStrategy beans found. Ensure strategies are correctly configured as Spring beans.");
        } else {
            // Create a map from the action type class to the strategy instance.
            // Uses a merge function to handle potential duplicate strategy registrations for the same Action type,
            // though ideally, each Action type should have only one strategy.
            strategyMap = strategies.stream()
                .collect(Collectors.toMap(ActionStrategy::getActionType, Function.identity(),
                    (existing, replacement) -> {
                        System.err.println("Warning: Duplicate strategy found for action type: " +
                            existing.getActionType().getName() +
                            ". Using existing strategy: " + existing.getClass().getName() +
                            ", ignoring duplicate: " + replacement.getClass().getName());
                        return existing; // Keep the existing strategy in case of duplicates
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
        // Check if strategyMap is initialized, which happens in @PostConstruct
        if (strategyMap == null) {
            throw new IllegalStateException("ActionStrategyRegistry's strategyMap is not initialized. " +
                "This might indicate an issue with Spring bean lifecycle or configuration.");
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
    
    /**
     * Retrieves the appropriate strategy for the given Action class.
     * This can be useful for checking if a strategy exists for a certain type without an instance.
     *
     * @param actionClass The Class of the Action for which to find a strategy.
     * @return The ActionStrategy capable of handling the provided action class.
     * @throws IllegalArgumentException if the actionClass is null or no strategy is found.
     * @throws IllegalStateException if the registry was not properly initialized.
     */
    public ActionStrategy getStrategy(Class<? extends Action> actionClass) {
        if (actionClass == null) {
            throw new IllegalArgumentException("Action class cannot be null when retrieving a strategy.");
        }
        if (strategyMap == null) {
            throw new IllegalStateException("ActionStrategyRegistry's strategyMap is not initialized.");
        }
        if (strategyMap.isEmpty()) {
            System.err.println("ActionStrategyRegistry has no strategies loaded. Cannot find strategy for class: " + actionClass.getName());
            throw new IllegalStateException("ActionStrategyRegistry contains no strategies.");
        }
        
        ActionStrategy strategy = strategyMap.get(actionClass);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for action class: " + actionClass.getName() +
                ". Available strategies are registered for types: " + strategyMap.keySet());
        }
        return strategy;
    }
}

