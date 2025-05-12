package com.ayago.testscenario.parser;

import com.ayago.testscenario.Action;
import com.ayago.testscenario.EnterAction;
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
