package com.ayago.smartuitest.engine;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements the ElementResolver interface using WebDriver to find elements
 * based on various strategies. This class encapsulates the element location logic.
 */
class WebDriverElementResolver implements ElementResolver {
    
    private final WebDriver driver;
    
    /**
     * Constructs the WebDriverElementResolver.
     * @param driver The WebDriver instance for browser interaction.
     */
    @Autowired
    WebDriverElementResolver(WebDriver driver) {
        this.driver = driver;
    }
    
    /**
     * Attempts to find a web element based on a field name using various strategies.
     *
     * @param fieldName The descriptive name of the field to find.
     * @return The located WebElement.
     * @throws IllegalArgumentException if fieldName is null or empty.
     * @throws RuntimeException if the field cannot be found using any strategy.
     */
    @Override
    public WebElement resolveField(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty for resolveField.");
        }
        System.out.println("WebDriverElementResolver: Attempting to resolve field: " + fieldName);
        
        // Strategy 1: Find by label's 'for' attribute (exact match on label text)
        try {
            WebElement label = driver.findElement(By.xpath("//label[normalize-space(.)='" + fieldName + "']"));
            String forId = label.getAttribute("for");
            if (forId != null && !forId.isEmpty()) {
                System.out.println("WebDriverElementResolver: Resolved field '" + fieldName + "' using label's 'for' attribute: " + forId);
                return driver.findElement(By.id(forId));
            }
        } catch (NoSuchElementException ignored) {
            // Try next strategy
        }
        
        // Strategy 2: Find by placeholder, name, or id (case-insensitive for name/id)
        try {
            String cleanFieldNameForAttr = fieldName.toLowerCase().replace(" ", "");
            WebElement element = driver.findElement(By.xpath(
                "//input[@placeholder='" + fieldName + "'] | " +
                    "//textarea[@placeholder='" + fieldName + "'] | " +
                    "//input[translate(@name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='" + cleanFieldNameForAttr + "'] | " +
                    "//textarea[translate(@name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='" + cleanFieldNameForAttr + "'] | " +
                    "//input[translate(@id, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='" + cleanFieldNameForAttr + "'] | " +
                    "//textarea[translate(@id, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='" + cleanFieldNameForAttr + "']"
            ));
            System.out.println("WebDriverElementResolver: Resolved field '" + fieldName + "' using placeholder, name, or id.");
            return element;
        } catch (NoSuchElementException ignored) {
            // Try next strategy
        }
        
        // Strategy 3: Label contains text, find following input or textarea sibling
        try {
            WebElement element = driver.findElement(By.xpath(
                "//label[contains(normalize-space(.),'" + fieldName + "')]/following-sibling::input[1] | " +
                    "//label[contains(normalize-space(.),'" + fieldName + "')]/following-sibling::textarea[1]"
            ));
            System.out.println("WebDriverElementResolver: Resolved field '" + fieldName + "' using label contains text and following-sibling.");
            return element;
        } catch (NoSuchElementException ignored) {
            // Try next strategy
        }
        
        // Strategy 4: Find by title attribute (exact match)
        try {
            WebElement element = driver.findElement(By.xpath("//*[@title='" + fieldName + "']"));
            System.out.println("WebDriverElementResolver: Resolved field '" + fieldName + "' using title attribute.");
            return element;
        } catch (NoSuchElementException ignored) {
            // Element not found by title
        }
        
        // Strategy 5: Find by aria-label attribute (exact match)
        try {
            WebElement element = driver.findElement(By.xpath("//*[@aria-label='" + fieldName + "']"));
            System.out.println("WebDriverElementResolver: Resolved field '" + fieldName + "' using aria-label attribute.");
            return element;
        } catch (NoSuchElementException ignored) {
            // Element not found by aria-label
        }
        
        System.err.println("WebDriverElementResolver: Field not found using any strategy: " + fieldName);
        throw new RuntimeException("Field not found: " + fieldName + " (WebDriverElementResolver)");
    }
}

