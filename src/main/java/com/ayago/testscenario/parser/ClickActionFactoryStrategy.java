package com.ayago.testscenario.parser;

import com.ayago.testscenario.Action;
import com.ayago.testscenario.ClickAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class ClickActionFactoryStrategy implements ActionFactoryStrategy{
    @Override
    public Action createAction(Map<String, String> data) {
        return new ClickAction(data.get("target"));
    }
    
    @Override
    public ActionType forActionType(){
        return ActionType.Click;
    }
}
