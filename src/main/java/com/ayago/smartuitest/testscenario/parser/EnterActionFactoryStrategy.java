package com.ayago.smartuitest.testscenario.parser;

import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.EnterAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class EnterActionFactoryStrategy implements ActionFactoryStrategy{
    @Override
    public Action createAction(Map<String, String> data) {
        return new EnterAction(data.get("targetField"), data.get("value"));
    }
    
    @Override
    public ActionType forActionType(){
        return ActionType.Enter;
    }
}
