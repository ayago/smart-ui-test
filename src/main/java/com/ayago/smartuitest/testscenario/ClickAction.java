package com.ayago.smartuitest.testscenario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ClickAction implements Action {
    @NotNull(message = "ClickAction target cannot be null")
    @NotBlank(message = "ClickAction target cannot be blank")
    private final String target;
    
    public ClickAction(String target) {
        this.target = target;
    }
    
    public String getTarget() {
        return target;
    }
    
    @Override
    public String toString() {
        return "ClickAction{" +
            "target='" + target + '\'' +
            '}';
    }
}
