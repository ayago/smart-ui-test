package com.ayago.smartuitest.engine;

import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.ClickAction;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

/**
 * Strategy for performing a 'Click' action on a web element.
 * It attempts to find common clickable elements (buttons, links) first,
 * and then falls back to the ElementResolver if needed.
 */
@Component
class ClickActionStrategy implements ActionStrategy {
    
    /**
     * Executes the click action.
     *
     * @param action   The action details, expected to be an instance of ClickAction.
     * @param resolver The ElementResolver to find web elements if direct lookup fails.
     * @throws IllegalArgumentException if the action is not a ClickAction or if the target is null/empty.
     * @throws RuntimeException         if the target element cannot be found or clicked.
     */
    @Override
    public void execute(Action action, ElementResolver resolver) {
        if (!(action instanceof ClickAction clickAction)) {
            throw new IllegalArgumentException("Action provided is not an instance of ClickAction: " + action.getClass().getName());
        }
        String target = clickAction.getTarget();
        
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("ClickAction target cannot be null or empty.");
        }
        
        WebElement elementToClick;
        try {
            // Prioritize finding common clickable elements directly.
            // Uses normalize-space() for robust text matching in buttons/links
            // and checks @value for input buttons/submits.
            // This XPath looks for:
            // 1. <button> elements with matching normalized text.
            // 2. <a> (link) elements with matching normalized text.
            // 3. <input type="button"> elements with matching @value attribute.
            // 4. <input type="submit"> elements with matching @value attribute.
            elementToClick = resolver.underlyingDriver().findElement(By.xpath(
                "//button[normalize-space(.)='" + target + "'] | " +
                    "//a[normalize-space(.)='" + target + "'] | " +
                    "//input[@type='button' and @value='" + target + "'] | " +
                    "//input[@type='submit' and @value='" + target + "']"
            ));
            System.out.println("ClickActionStrategy: Found target '" + target + "' as a common clickable element (button/link/input[@type='button' or @type='submit']).");
        } catch (NoSuchElementException e) {
            // If not found as a common clickable element, try resolving it using the ElementResolver.
            // This allows clicking on other elements that might be identified by resolveField's logic
            // (e.g., an element found via its label, placeholder, id, name, title, or aria-label).
            System.out.println("ClickActionStrategy: Target '" + target + "' not found as a common clickable element. Attempting to resolve via ElementResolver.");
            try {
                elementToClick = resolver.resolveField(target);
            } catch (RuntimeException re) {
                // Chain the exception to provide context.
                throw new RuntimeException("Clickable element or field not found for target: '" + target + "'. Resolver error: " + re.getMessage(), re);
            }
        }
        
        // Perform the click action.
        try {
            elementToClick.click();
            System.out.println("Successfully executed ClickAction on target: " + target);
        } catch (Exception e) {
            // Catch broader exceptions during click (e.g., ElementNotInteractableException)
            throw new RuntimeException("Failed to click on target: '" + target + "'. Element found but click failed. Error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Specifies that this strategy handles actions of type {@link ClickAction}.
     *
     * @return The {@code ClickAction.class} object.
     */
    @Override
    public Class<? extends Action> getActionType() {
        return ClickAction.class;
    }
}

