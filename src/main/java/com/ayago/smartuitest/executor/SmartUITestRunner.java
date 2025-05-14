package com.ayago.smartuitest.executor;


import com.ayago.smartuitest.engine.WebInteractionEngine;
import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.TestScenario;
import com.ayago.smartuitest.testscenario.TestScenario.ExpectedElement;
import com.ayago.smartuitest.testscenario.TestScenario.Page;
import com.ayago.smartuitest.testscenario.parser.TestScenarioParser;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class SmartUITestRunner implements CommandLineRunner{
    private final TestScenarioParser parser;
    private final WebDriver driver;
    private final WebInteractionEngine interactionEngine;
    private final FeatureManagerClient featureManager;
    
    @Autowired
    public SmartUITestRunner(
        TestScenarioParser parser,
        WebDriver driver,
        WebInteractionEngine interactionEngine,
        FeatureManagerClient featureManager
    ){
        this.parser = parser;
        this.driver = driver;
        this.interactionEngine = interactionEngine;
        this.featureManager = featureManager;
    }
    
    @Override
    public void run(String... args) throws Exception{
        TestScenario definition = parser.parse("dashboard-flow.txt");
        
        System.out.println("Target Host: " + definition.getHost());
        featureManager.applyFeatureFlags(definition.getFeatures());
        
        for (Page page : definition.getPages()){
            driver.get(definition.getHost()); // Replace with host + relative path if needed
            
            for (ExpectedElement expected : page.getExpected()){
                String actualValue = interactionEngine.getFieldValue(expected.getTarget());
                if (!actualValue.equals(expected.getValue())) {
                    throw new AssertionError(
                        "Expected field '" + expected.getTarget() + "' to be '" + expected.getValue() + "' but found '" +
                            actualValue + "'");
                }
            }
            
            Action action = page.getAction();
            interactionEngine.performAction(action);
        }
        
        driver.quit();
    }
    
}
