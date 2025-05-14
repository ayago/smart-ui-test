package com.ayago.smartuitest.testscenario.parser;

import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.SubmitAction;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
class SubmitActionFactoryStrategy implements ActionFactoryStrategy{
    @Override
    public Action createAction(Map<String, String> data) {
        Map<String, String> submitData = new HashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getKey().startsWith("fields.")) {
                String key = entry.getKey().substring("fields.".length());
                submitData.put(key, entry.getValue());
            }
        }
        return new SubmitAction(submitData);
    }
    
    @Override
    public ActionType forActionType(){
        return ActionType.Submit;
    }
}
