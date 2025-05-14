package com.ayago.smartuitest.testscenario.json;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Contains Jackson MixIn definitions for the concrete Action implementation classes
 * in {@link com.ayago.testscenario}.
 * This allows adding Jackson annotations without modifying the original POJOs.
 */
public class ActionMixIns{
    
    /**
     * Jackson MixIn for the {@link com.ayago.smartuitest.testscenario.ClickAction} class.
     */
    public abstract static class ClickActionMixIn {
        /**
         * MixIn constructor for Jackson to use when deserializing ClickAction.
         *
         * @param target The target element or identifier for the click action.
         */
        @JsonCreator
        public ClickActionMixIn(@JsonProperty("target") String target) {
            // This constructor is for Jackson's mapping.
            // The actual ClickAction constructor will be called.
        }
    }
    
    /**
     * Jackson MixIn for the {@link com.ayago.smartuitest.testscenario.EnterAction} class.
     */
    public abstract static class EnterActionMixIn {
        /**
         * MixIn constructor for Jackson to use when deserializing EnterAction.
         *
         * @param targetField The target input field where text will be entered.
         * @param value       The text value to be entered into the target field.
         */
        @JsonCreator
        public EnterActionMixIn(
            @JsonProperty("targetField") String targetField,
            @JsonProperty("value") String value) {
            // This constructor is for Jackson's mapping.
            // The actual EnterAction constructor will be called.
        }
    }
    
    /**
     * Jackson MixIn for the {@link com.ayago.smartuitest.testscenario.SubmitAction} class.
     */
    public abstract static class SubmitActionMixIn {
        /**
         * MixIn constructor for Jackson to use when deserializing SubmitAction.
         *
         * @param fields A map of field names to values that should be filled
         * as part of the submit action.
         */
        @JsonCreator
        public SubmitActionMixIn(@JsonProperty("fields") Map<String, String> fields) {
            // This constructor is for Jackson's mapping.
            // The actual SubmitAction constructor will be called.
        }
    }
}

