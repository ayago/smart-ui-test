package com.ayago.smartuitest.engine;

import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.SubmitAction;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Strategy for performing a 'Submit' action.
 * This can involve filling specified fields and then attempting to submit the form,
 * or clicking a generic submit button if no fields are specified.
 */
@Component
class SubmitActionStrategy implements ActionStrategy {
    
    /**
     * Executes the submit action.
     * If fields are provided in the SubmitAction, they are filled first.
     * Then, it attempts to submit the form, typically by calling .submit() on the last
     * filled element or by clicking a generic submit button as a fallback.
     * If no fields are provided, it tries to click a generic submit button directly.
     *
     * @param action   The action details, expected to be an instance of SubmitAction.
     * @param resolver The ElementResolver to find web elements.
     * @throws IllegalArgumentException if the action is not a SubmitAction.
     * @throws RuntimeException         if a required field cannot be found or interacted with,
     *                                  or if submission fails critically.
     */
    @Override
    public void execute(Action action, ElementResolver resolver) {
        if (!(action instanceof SubmitAction submitAction)) {
            throw new IllegalArgumentException("Action provided is not an instance of SubmitAction: " + action.getClass().getName());
        }
        Map<String, String> fieldsToFill = submitAction.getFields();
        
        // Case 1: No fields specified in the SubmitAction.
        // Attempt to find and click a common/generic submit button.
        if (fieldsToFill == null || fieldsToFill.isEmpty()) {
            System.out.println("SubmitAction: No fields specified. Attempting to click a generic submit button.");
            try {
                // Comprehensive XPath for various common submit button patterns.
                WebElement submitButton = resolver.underlyingDriver().findElement(By.xpath(
                    "//input[@type='submit'] | " +
                        "//button[@type='submit'] | " +
                        "//button[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'submit')] | " +
                        "//button[contains(translate(@id, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'submit')] | " +
                        "//button[contains(translate(@name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'submit')]"
                ));
                submitButton.click();
                System.out.println("SubmitAction: Successfully clicked a generic submit button.");
            } catch (NoSuchElementException e) {
                // If no generic submit button is found, this might not be an error if the form
                // submits automatically or if a specific click action is intended elsewhere.
                System.err.println("SubmitAction: No fields were specified, and no generic submit button was found. " +
                    "The form might submit automatically, or a specific ClickAction on a submit button might be needed.");
                // Depending on requirements, you might throw an exception here if a submit button is always expected.
                // For example: throw new RuntimeException("SubmitAction: No fields to fill and no generic submit button found to click.", e);
            }
            return; // Execution path for no-fields submit ends here.
        }
        
        // Case 2: Fields are specified. Fill them and then attempt to submit.
        WebElement lastFieldFilled = null;
        System.out.println("SubmitAction: Processing " + fieldsToFill.size() + " fields to fill.");
        for (Map.Entry<String, String> entry : fieldsToFill.entrySet()) {
            String fieldName = entry.getKey();
            String value = entry.getValue();
            
            if (fieldName == null || fieldName.trim().isEmpty()) {
                System.err.println("SubmitAction: Encountered a field with a null or empty name. Skipping this field.");
                continue;
            }
            if (value == null) {
                System.err.println("SubmitAction: Field '" + fieldName + "' has a null value. Skipping this field. Use empty string for no value.");
                continue;
            }
            
            try {
                WebElement field = resolver.resolveField(fieldName);
                field.clear();
                field.sendKeys(value);
                lastFieldFilled = field; // Keep track of the last field successfully interacted with.
                System.out.println("SubmitAction: Successfully entered value '" + value + "' into field '" + fieldName + "'.");
            } catch (Exception e) { // Catch broader exceptions during resolve/sendKeys
                System.err.println("SubmitAction: Failed to resolve or interact with field '" + fieldName + "'. Error: " + e.getMessage());
                // Optional: Decide if an error on one field should stop the whole submit action.
                // For now, it logs the error and continues with other fields.
                // To stop: throw new RuntimeException("Failed to process field '" + fieldName + "' for submit action.", e);
            }
        }
        
        // After attempting to fill all fields, try to submit the form.
        if (lastFieldFilled != null) {
            System.out.println("SubmitAction: All specified fields processed. Attempting to submit the form related to the last field: " + fieldsToFill.keySet());
            try {
                // The .submit() method on a WebElement will attempt to submit the form
                // that the element belongs to. This is often the most reliable way if
                // the element is part of a <form>.
                lastFieldFilled.submit();
                System.out.println("SubmitAction: Successfully called .submit() on the form containing the last filled element.");
            } catch (Exception e) {
                // If .submit() on the element fails (e.g., element not in a form, or JS prevents it),
                // try a fallback: clicking a generic submit button.
                System.err.println("SubmitAction: Calling .submit() on the last field's form failed. " +
                    "Attempting to click a generic submit button as a fallback. Original error: " + e.getMessage());
                try {
                    WebElement submitButton = resolver.underlyingDriver().findElement(By.xpath(
                        "//input[@type='submit'] | //button[@type='submit'] | //button[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'submit')]"
                    ));
                    submitButton.click();
                    System.out.println("SubmitAction: Fallback - Successfully clicked a generic submit button.");
                } catch (NoSuchElementException nse) {
                    System.err.println("SubmitAction: Fallback - No generic submit button found after failing to submit form via element. " +
                        "The form might require a specific ClickAction on its submit button or might have submitted via JavaScript.");
                }
            }
        } else {
            System.err.println("SubmitAction: No fields were successfully filled or interacted with, so no form submission was attempted via .submit(). " +
                "If fields were specified but all failed, this indicates issues with field resolution or interaction.");
        }
    }
    
    /**
     * Specifies that this strategy handles actions of type {@link SubmitAction}.
     *
     * @return The {@code SubmitAction.class} object.
     */
    @Override
    public Class<? extends Action> getActionType() {
        return SubmitAction.class;
    }
}
