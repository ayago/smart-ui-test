package com.ayago.smartuitest.engine;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Component;

@Component
public class WebInteractionEngineFactory{
    
    private final ActionStrategyRegistry strategyRegistry;
    
    public WebInteractionEngineFactory(ActionStrategyRegistry strategyRegistry){
        this.strategyRegistry = strategyRegistry;
    }
    
    public WebInteractionEngine create(String host){
        WebDriver webDriver = new ChromeDriver();
        webDriver.get(host);
        return new WebInteractionEngine(webDriver, strategyRegistry);
    }
}
