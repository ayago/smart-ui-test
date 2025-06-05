package com.ayago.smartuitest.engine;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class WebInteractionEngineFactory{
    
    private final ActionStrategyRegistry strategyRegistry;
    
    public WebInteractionEngineFactory(ActionStrategyRegistry strategyRegistry){
        this.strategyRegistry = strategyRegistry;
    }
    
    public WebInteractionEngine create(WebDriver webDriver, String host){
        webDriver.get(host);
        waitForPageToLoad(webDriver, 15);
        return new WebInteractionEngine(webDriver, strategyRegistry);
    }
    
    public void waitForPageToLoad(WebDriver driver, long timeoutInSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
        
        // Custom ExpectedCondition to check document.readyState
        ExpectedCondition<Boolean> pageLoadCondition =
            driver1 -> ((JavascriptExecutor) driver1).executeScript("return document.readyState").equals("complete");
        wait.until(pageLoadCondition);
    }
}
