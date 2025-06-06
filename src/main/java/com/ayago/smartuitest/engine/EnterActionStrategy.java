package com.ayago.smartuitest.engine;

import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.EnterAction;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions; // Import ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait; // Import WebDriverWait
import org.springframework.stereotype.Component;

import java.time.Duration; // Import Duration

/**
 * Strategy for performing an 'Enter' action (typing text into a field).
 * It uses the ElementResolver to find the target field and then
 * clears it and sends the specified keys. Includes explicit waiting
 * to ensure the element is interactable.
 */
@Component
class EnterActionStrategy implements ActionStrategy {
    
    /**
     * Executes the enter (type text) action.
     * Waits for the target element to be visible and clickable before interacting.
     *
     * @param action   The action details, expected to be an instance of EnterAction.
     * @param resolver The ElementResolver to find the target web element.
     * must be added to the ActionStrategy interface and passed
     * by the calling code (e.g., WebInteractionEngine).
     * @throws IllegalArgumentException if the action is not an EnterAction, or if targetField/value are invalid.
     * @throws RuntimeException         if the target field cannot be found or interacted with
     * (including timeout during waiting).
     */
    @Override
    public void execute(Action action, ElementResolver resolver) { // Added WebDriver parameter
        if (!(action instanceof EnterAction enterAction)) {
            throw new IllegalArgumentException("Action provided is not an instance of EnterAction: " + action.getClass().getName());
        }
        String targetField = enterAction.getTargetField();
        String value = getValueToUse(enterAction, targetField); // Validate targetField and value
        
        // Resolve the field using the provided ElementResolver.
        // Note: resolver.resolveField should ideally handle basic NoSuchElementException,
        // but the wait below handles interactability issues after finding the element.
        WebElement field = resolver.resolveField(targetField);
        
        // Add explicit wait for the element to be clickable (visible and enabled)
        // Adjust the timeout (10 seconds here) based on your application's performance.
        WebDriverWait wait = new WebDriverWait(resolver.underlyingDriver(), Duration.ofSeconds(15));
        
        try {
            // Wait until the element is visible and enabled so we can interact.
            // ExpectedConditions.elementToBeClickable is a good choice for input fields.
            wait.until(ExpectedConditions.elementToBeClickable(field));
            
            field.clear(); // Clear the field before sending new keys.
            field.sendKeys(value, Keys.ENTER); // Send the keys.
            System.out.println("Successfully executed EnterAction on targetField: " + targetField + " with value: '" + value + "'");
        } catch (Exception e) {
            // Catch exceptions during the wait (TimeoutException) or sendKeys (ElementNotInteractableException, etc.)
            throw new RuntimeException("Failed to enter text into targetField: '" + targetField + "'. Error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates and returns the value to be used for the EnterAction.
     *
     * @param enterAction The EnterAction object.
     * @param targetField The target field identifier.
     * @return The value to send to the element.
     * @throws IllegalArgumentException if targetField or value are null/empty.
     */
    private String getValueToUse(EnterAction enterAction, String targetField){
        String value = enterAction.getValue();
        
        if (targetField == null || targetField.trim().isEmpty()) {
            throw new IllegalArgumentException("EnterAction targetField cannot be null or empty.");
        }
        // A null value for sendKeys might cause issues with WebDriver.
        // An empty string is acceptable to clear a field or type nothing.
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
    
    @Override
    public void execute(Action action, Runnable executeBefore, ElementResolver resolver){
        if (!(action instanceof EnterAction enterAction)) {
            throw new IllegalArgumentException("Action provided is not an instance of EnterAction: " + action.getClass().getName());
        }
        String targetField = enterAction.getTargetField();
        String value = getValueToUse(enterAction, targetField); // Validate targetField and value
        
        // Resolve the field using the provided ElementResolver.
        // Note: resolver.resolveField should ideally handle basic NoSuchElementException,
        // but the wait below handles interactability issues after finding the element.
        WebElement field = resolver.resolveField(targetField);
        
        try {
            Actions actions = new Actions(resolver.underlyingDriver());
            actions.scrollToElement(field).build().perform();
            executeBefore.run();
            field.clear(); // Clear the field before sending new keys.
            field.sendKeys(value, Keys.ENTER); // Send the keys.
            System.out.println("Successfully executed EnterAction on targetField: " + targetField + " with value: '" + value + "'");
        } catch (Exception e) {
            // Catch exceptions during the wait (TimeoutException) or sendKeys (ElementNotInteractableException, etc.)
            throw new RuntimeException("Failed to enter text into targetField: '" + targetField + "'. Error: " + e.getMessage(), e);
        }
    }
}
