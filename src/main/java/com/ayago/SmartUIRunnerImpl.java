package com.ayago;

import com.ayago.pageflow.TestScenario;
import com.ayago.pageflow.PageFlowParser;
import com.ayago.pageflow.Page;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class SmartUIRunnerImpl implements CommandLineRunner{
    private final PageFlowParser parser;
    private final WebDriver driver;
    private final SmartLocatorEngine locator;
    private final FeatureManagerClient featureManager;
    
    @Autowired
    public SmartUIRunnerImpl(
        PageFlowParser parser,
        WebDriver driver,
        SmartLocatorEngine locator,
        FeatureManagerClient featureManager
    ){
        this.parser = parser;
        this.driver = driver;
        this.locator = locator;
        this.featureManager = featureManager;
    }
    
    @Override
    public void run(String... args) throws Exception{
        TestScenario definition = parser.parse("dashboard-flow.txt");
        
        System.out.println("Target Host: " + definition.getHost());
        featureManager.applyFeatureFlags(definition.getFeatures());
        
        for (Page page : definition.getPages()){
            driver.get(definition.getHost()); // Replace with host + relative path if needed
            
            for (Map.Entry<String, String> expected : page.getExpectedFields().entrySet()){
                WebElement input = locator.resolveField(expected.getKey());
                String actualValue = input.getAttribute("value");
                if (!actualValue.equals(expected.getValue())) {
                    throw new AssertionError(
                        "Expected field '" + expected.getKey() + "' to be '" + expected.getValue() + "' but found '" +
                            actualValue + "'");
                }
            }
            
            for (Map.Entry<String, String> field : page.getGivenFieldValues().entrySet()){
                WebElement input = locator.resolveField(field.getKey());
                input.clear();
                input.sendKeys(field.getValue());
            }
            
            locator.clickButton(page.getActionButton());
        }
        
        driver.quit();
    }
    
}
