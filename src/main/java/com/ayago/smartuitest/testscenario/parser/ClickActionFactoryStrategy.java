package com.ayago.smartuitest.testscenario.parser;

import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.ClickAction;
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
