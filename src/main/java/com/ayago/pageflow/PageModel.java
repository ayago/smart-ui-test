package com.ayago.pageflow;

import java.util.Map;
import java.util.Objects;

public class PageModel {
    private final String name;
    private final Map<String, String> expectedFields;
    private final Map<String, String> givenFieldValues;
    private final String actionButton;
    
    public PageModel(String name, Map<String, String> expectedFields, Map<String, String> givenFieldValues, String actionButton) {
        this.name = name;
        this.expectedFields = expectedFields;
        this.givenFieldValues = givenFieldValues;
        this.actionButton = actionButton;
    }
    
    
    public String getName() {
        return name;
    }
    
    public Map<String, String> getExpectedFields() {
        return expectedFields;
    }
    
    public Map<String, String> getGivenFieldValues() {
        return givenFieldValues;
    }
    
    public String getActionButton() {
        return actionButton;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageModel pageModel = (PageModel) o;
        return Objects.equals(name, pageModel.name) && Objects.equals(expectedFields, pageModel.expectedFields) && Objects.equals(givenFieldValues, pageModel.givenFieldValues) && Objects.equals(actionButton, pageModel.actionButton);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, expectedFields, givenFieldValues, actionButton);
    }
}

