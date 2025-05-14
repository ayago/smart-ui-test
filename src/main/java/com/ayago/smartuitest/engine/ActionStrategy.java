package com.ayago.smartuitest.engine;

import com.ayago.smartuitest.testscenario.Action;

/**
 * Defines the contract for executing a specific web browser action.
 * Implementations of this interface will handle concrete action types
 * like Click, Enter, Submit.
 */
public interface ActionStrategy {
    /**
     * Executes the specific web action.
     *
     * @param action   The action details derived from the test scenario.
     * @param resolver The ElementResolver instance to find web elements.
     */
    void execute(Action action, ElementResolver resolver);
    
    /**
     * Returns the concrete Action class (e.g., ClickAction.class) that this strategy handles.
     * This is used by the registry to map action types to strategies.
     *
     * @return The Class object of the Action type this strategy is responsible for.
     */
    Class<? extends Action> getActionType();
}

