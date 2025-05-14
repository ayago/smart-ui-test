package com.ayago.smartuitest.engine;

import com.ayago.smartuitest.testscenario.Action;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Engine responsible for orchestrating web actions.
 * It uses an ActionStrategyRegistry to find the correct strategy for an action
 * and an internally managed WebDriverElementResolver to locate elements on the page,
 * which is then passed to the strategies.
 */
@Component
public class WebInteractionEngine {
    private final ActionStrategyRegistry strategyRegistry;
    private final ElementResolver elementResolver; // Instance of WebDriverElementResolver
    
    /**
     * Constructs the SmartLocatorEngine.
     * It initializes its own WebDriverElementResolver instance.
     *
     * @param driver The WebDriver instance for browser interaction.
     * @param strategyRegistry The registry that provides action execution strategies.
     */
    @Autowired
    public WebInteractionEngine(WebDriver driver, ActionStrategyRegistry strategyRegistry) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver instance cannot be null for SmartLocatorEngine.");
        }
        if (strategyRegistry == null) {
            throw new IllegalArgumentException("ActionStrategyRegistry cannot be null for SmartLocatorEngine.");
        }
        this.strategyRegistry = strategyRegistry;
        // Instantiate WebDriverElementResolver directly, passing the WebDriver instance.
        // This elementResolver will be used by the strategies.
        this.elementResolver = new WebDriverElementResolver(driver);
    }
    
    /**
     * Performs a web action based on the type of Action object provided.
     * It retrieves the appropriate strategy from the {@link ActionStrategyRegistry}
     * and delegates the execution to it, providing the necessary WebDriver and ElementResolver.
     *
     * @param action The Action object (e.g., ClickAction, EnterAction, SubmitAction).
     * @throws RuntimeException if the action cannot be performed (e.g., element not found, or strategy execution fails).
     * @throws IllegalArgumentException if the provided action is null or no strategy is found for its type.
     */
    public void performAction(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("Action to perform cannot be null.");
        }
        System.out.println("SmartLocatorEngine: Attempting to perform action: " + action.toString());
        
        // Retrieve the appropriate strategy based on the action's concrete type
        ActionStrategy strategy = strategyRegistry.getStrategy(action);
        
        // Execute the action using the chosen strategy.
        // Pass the WebDriver and the internally managed ElementResolver instance.
        // The strategy will use this elementResolver to call resolveField.
        strategy.execute(action, this.elementResolver);
        
        System.out.println("SmartLocatorEngine: Action performed successfully: " + action.toString());
    }
    
    public String getFieldValue(String fieldName){
        WebElement webElement = this.elementResolver.resolveField(fieldName);
        return webElement.getAttribute("value");
    }
}

