package com.ayago.smartuitest.testscenario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Valid
public class TestScenario {
    
    @NotNull(message = "Host cannot be null")
    @NotBlank(message = "Host cannot be blank")
    private final String host;
    
    private final Map<String, Feature> features;
    
    @NotNull(message = "Pages cannot be null")
    @Valid
    private final List<Page> pages;
    
    public TestScenario(String host, Map<String, Feature> features, List<Page> pages) {
        this.host = host;
        this.features = features;
        this.pages = pages;
    }
    
    public String getHost() {
        return host;
    }
    
    public Map<String, Feature> getFeatures() {
        return features;
    }
    
    public List<Page> getPages() {
        return pages;
    }
    
    @Override
    public String toString() {
        return "TestScenario{" +
            "host='" + host + '\'' +
            ", features=" + features +
            ", pages=" + pages +
            '}';
    }
    
    public static class Feature {
        private final boolean enable;
        private final String name;
        private final Map<String, String> context;
        
        public Feature(boolean enable, Map<String, String> context, String name) {
            this.enable = enable;
            this.name = name;
            this.context = context;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isEnable() {
            return enable;
        }
        
        public Map<String, String> getContext() {
            return context;
        }
        
        @Override
        public String toString() {
            return "Feature{" +
                "enable=" + enable +
                ", name='" + name + '\'' +
                ", context=" + context +
                '}';
        }
    }
    
    public static class Page {
        @NotNull(message = "Page name cannot be null")
        @NotBlank(message = "Page name cannot be blank")
        private final String name;
        
        @NotNull(message = "Page must have at least one expected element")
        @Valid
        private final List<ExpectedElement> expected;
        
        @Valid
        private final Action action;
        
        public Page(String name, List<ExpectedElement> expected, Action action) {
            this.name = name;
            this.expected = CollectionUtils.isEmpty(expected) ? Collections.emptyList() : expected;
            this.action = action;
        }
        
        public String getName() {
            return name;
        }
        
        public List<ExpectedElement> getExpected() {
            return expected;
        }
        
        public Action getAction() {
            return action;
        }
        
        @Override
        public String toString() {
            return "Page{" +
                "name='" + name + '\'' +
                ", expected=" + expected +
                ", action=" + action +
                '}';
        }
    }
    
    public static class ExpectedElement {
        @NotNull(message = "Expected target cannot be null")
        @NotBlank(message = "Expected target cannot be blank")
        private final String target;
        
        @NotNull(message = "Expected value cannot be null")
        @NotBlank(message = "Expected value cannot be blank")
        private final String value;
        
        public ExpectedElement(String target, String value) {
            this.target = target;
            this.value = value;
        }
        
        public String getTarget() {
            return target;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return "ExpectedElement{" +
                "target='" + target + '\'' +
                ", value='" + value + '\'' +
                '}';
        }
    }
}



