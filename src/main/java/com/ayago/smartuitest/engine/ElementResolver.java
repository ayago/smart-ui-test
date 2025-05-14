package com.ayago.smartuitest.engine;

import org.openqa.selenium.WebElement;

/**
 * Defines a contract for resolving web elements based on a descriptive name.
 * This interface is used to decouple action strategies from the concrete
 * implementation of element location.
 */
interface ElementResolver {
    /**
     * Attempts to find a web element based on a field name.
     *
     * @param fieldName The descriptive name of the field to find.
     * @return The located WebElement.
     * @throws IllegalArgumentException if fieldName is null or empty.
     * @throws RuntimeException if the field cannot be found.
     */
    WebElement resolveField(String fieldName);
}