package com.ayago.testscenario;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class SubmitAction implements Action {
    @NotNull(message = "SubmitAction fields cannot be null")
    private final Map<String, String> fields;
    
    public SubmitAction(Map<String, String> fields) {
        this.fields = fields;
    }
    
    public Map<String, String> getFields() {
        return fields;
    }
    
    @Override
    public String toString() {
        return "SubmitAction{" +
            "fields=" + fields +
            '}';
    }
}
