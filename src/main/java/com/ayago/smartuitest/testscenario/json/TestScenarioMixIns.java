package com.ayago.smartuitest.testscenario.json;


import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.TestScenario.ExpectedElement;
import com.ayago.smartuitest.testscenario.TestScenario.Feature;
import com.ayago.smartuitest.testscenario.TestScenario.Page;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Jackson MixIn for the {@link com.ayago.smartuitest.testscenario.TestScenario} class.
 * This allows adding Jackson annotations without modifying the original POJO.
 * It specifies how to deserialize TestScenario objects using its constructor.
 */
public abstract class TestScenarioMixIns{
    
    /**
     * MixIn constructor for Jackson to use when deserializing TestScenario.
     *
     * @param host The host URL for the test scenario.
     * @param features A map of features, where the key is the feature name.
     * @param pages A list of pages in the test scenario.
     */
    @JsonCreator
    public TestScenarioMixIns(
        @JsonProperty("host") String host,
        @JsonProperty("features") Map<String, Feature> features,
        @JsonProperty("pages") List<Page> pages) {
        // This constructor is just for Jackson's benefit to know the mapping.
        // The actual TestScenario constructor will be called.
    }
    
    /**
     * Jackson MixIn for the {@link com.ayago.smartuitest.testscenario.TestScenario.ExpectedElement} class.
     */
    public abstract static class ExpectedElementMixIn{
        /**
         * MixIn constructor for Jackson to use when deserializing ExpectedElement.
         *
         * @param target The target element or state being checked.
         * @param value  The expected value or condition of the target.
         */
        @JsonCreator
        public ExpectedElementMixIn(
            @JsonProperty("target") String target,
            @JsonProperty("value") String value
        ){
            // This constructor is for Jackson's mapping.
            // The actual TestScenario.ExpectedElement constructor will be called.
        }
    }
    
    public abstract static class FeatureMixIn{
        /**
         * MixIn constructor for Jackson to use when deserializing Feature.
         *
         * @param enable  Indicates if the feature is enabled.
         * @param context A map of context-specific properties for the feature.
         * @param name    The name of the feature. This is often the key in the 'features' map
         *                in the parent TestScenario JSON, but can also be explicitly included
         *                as a property within the Feature JSON object if desired.
         */
        @JsonCreator
        public FeatureMixIn(
            @JsonProperty("enable") boolean enable,
            @JsonProperty("context") Map<String, String> context,
            @JsonProperty("name") String name
        ){
            // This constructor is for Jackson's mapping.
            // The actual TestScenario.Feature constructor will be called.
        }
    }
    
    /**
     * Jackson MixIn for the {@link Page} class.
     */
    public abstract static class PageMixIn{
        /**
         * MixIn constructor for Jackson to use when deserializing Page.
         *
         * @param name     The name of the page.
         * @param expected A list of expected elements or states on the page.
         * @param action   The action to be performed on this page.
         */
        @JsonCreator
        public PageMixIn(
            @JsonProperty("name") String name,
            @JsonProperty("expected") List<ExpectedElement> expected,
            @JsonProperty("action") Action action
        ){
            // This constructor is for Jackson's mapping.
            // The actual TestScenario.Page constructor will be called.
        }
    }
}