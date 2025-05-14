package com.ayago.smartuitest.testscenario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EnterAction implements Action {
    @NotNull(message = "EnterAction targetField cannot be null")
    @NotBlank(message = "EnterAction targetField cannot be blank")
    private final String targetField;
    
    @NotNull(message = "EnterAction value cannot be null")
    @NotBlank(message = "EnterAction value cannot be blank")
    private final String value;
    
    public EnterAction(String targetField, String value) {
        this.targetField = targetField;
        this.value = value;
    }
    
    public String getTargetField() {
        return targetField;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return "EnterAction{" +
            "targetField='" + targetField + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
