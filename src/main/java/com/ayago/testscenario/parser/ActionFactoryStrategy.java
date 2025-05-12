package com.ayago.testscenario.parser;

import com.ayago.testscenario.Action;

import java.util.Map;

public interface ActionFactoryStrategy{
    Action createAction(Map<String, String> data);
    
    ActionType forActionType();
}
