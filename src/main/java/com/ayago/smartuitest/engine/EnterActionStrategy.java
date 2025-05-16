package com.ayago.smartuitest.engine;

import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.EnterAction;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

/**
 * Strategy for performing an 'Enter' action (typing text into a field).
 * It uses the ElementResolver to find the target field and then
 * clears it and sends the specified keys.
 */
@Component
class EnterActionStrategy implements ActionStrategy {
    
    /**
     * Executes the enter (type text) action.
     *
     * @param action   The action details, expected to be an instance of EnterAction.
     * @param resolver The ElementResolver to find the target web element.
     * @throws IllegalArgumentException if the action is not an EnterAction, or if targetField/value are invalid.
     * @throws RuntimeException         if the target field cannot be found or interacted with.
     */
    @Override
    public void execute(Action action, ElementResolver resolver) {
        if (!(action instanceof EnterAction enterAction)) {
            throw new IllegalArgumentException("Action provided is not an instance of EnterAction: " + action.getClass().getName());
        }
        String targetField = enterAction.getTargetField();
        String value = getValueToUse(enterAction, targetField);
        
        // Resolve the field using the provided ElementResolver.
        WebElement field = resolver.resolveField(targetField);
        
        try {
            field.clear(); // It's good practice to clear the field before sending new keys.
            field.sendKeys(value);
            System.out.println("Successfully executed EnterAction on targetField: " + targetField + " with value: '" + value + "'");
        } catch (Exception e) {
            // Catch broader exceptions during sendKeys (e.g., ElementNotInteractableException)
            throw new RuntimeException("Failed to enter text into targetField: '" + targetField + "'. Element found but sendKeys failed. Error: " + e.getMessage(), e);
        }
    }
    
    private String getValueToUse(EnterAction enterAction, String targetField){
        String value = enterAction.getValue();
        
        if (targetField == null || targetField.trim().isEmpty()) {
            throw new IllegalArgumentException("EnterAction targetField cannot be null or empty.");
        }
        // A null value for sendKeys might cause issues with WebDriver,
        // but an empty string is often used to clear a field or type nothing.
        if (value == null) {
            throw new IllegalArgumentException("EnterAction value cannot be null (use an empty string to type nothing or clear).");
        }
        return value;
    }
    
    /**
     * Specifies that this strategy handles actions of type {@link EnterAction}.
     *
     * @return The {@code EnterAction.class} object.
     */
    @Override
    public Class<? extends Action> getActionType() {
        return EnterAction.class;
    }
}

