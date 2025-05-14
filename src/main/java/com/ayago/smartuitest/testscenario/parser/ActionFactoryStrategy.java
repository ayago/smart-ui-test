package com.ayago.smartuitest.testscenario.parser;

import com.ayago.smartuitest.testscenario.Action;

import java.util.Map;

public interface ActionFactoryStrategy{
    Action createAction(Map<String, String> data);
    
    ActionType forActionType();
}
