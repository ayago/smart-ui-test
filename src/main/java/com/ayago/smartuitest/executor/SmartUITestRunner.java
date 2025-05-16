package com.ayago.smartuitest.executor;

import com.ayago.smartuitest.engine.WebInteractionEngine;
import com.ayago.smartuitest.engine.WebInteractionEngineFactory;
import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.TestScenario;
import com.ayago.smartuitest.testscenario.TestScenario.ExpectedElement;
import com.ayago.smartuitest.testscenario.TestScenario.Page;
import com.ayago.smartuitest.testscenario.json.JsonTestScenarioParser;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.Files.walk;

@Component
class SmartUITestRunner implements CommandLineRunner {
    private final JsonTestScenarioParser parser;
    private final WebDriver driver;
    private final WebInteractionEngineFactory webInteractionEngineFactory;
    private final FeatureManagerClient featureManager; // Assuming FeatureManagerClient exists
    
    public SmartUITestRunner(
        JsonTestScenarioParser parser,
        WebDriver driver,
        WebInteractionEngineFactory webInteractionEngineFactory,
        FeatureManagerClient featureManager // Assuming FeatureManagerClient exists
    ) {
        this.parser = parser;
        this.driver = driver;
        this.webInteractionEngineFactory = webInteractionEngineFactory;
        this.featureManager = featureManager; // Assuming FeatureManagerClient exists
    }
    
    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.out.println("Please provide the directory path containing JSON test scenario files as an argument.");
            return;
        }
        
        String directoryPath = args[0];
        File directory = new File(directoryPath);
        
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Invalid directory path provided: " + directoryPath);
            return;
        }
        
        List<File> jsonFiles = walk(Paths.get(directoryPath))
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".json"))
            .map(Path::toFile)
            .toList();
        
        if (jsonFiles.isEmpty()) {
            System.out.println("No JSON files found in the specified directory: " + directoryPath);
            return;
        }
        
        for (File jsonFile : jsonFiles) {
            System.out.println("Running test scenario from file: " + jsonFile.getAbsolutePath());
            runTestScenario(jsonFile);
        }
        driver.quit(); // Quit driver after all files are processed
    }
    
    private void runTestScenario(File jsonFile){
        try {
            TestScenario definition = parser.parse(jsonFile); // Modified to pass File object
            
            WebInteractionEngine interactionEngine = webInteractionEngineFactory.create(definition.getHost());
            System.out.println("Target Host: " + definition.getHost());
            featureManager.applyFeatureFlags(definition.getFeatures()); // Assuming applyFeatureFlags exists
            
            for (Page page : definition.getPages()) {
                driver.get(definition.getHost()); // Replace with host + relative path if needed
                
                for (ExpectedElement expected : page.getExpected()) {
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
            
        } catch (Exception e) {
            System.err.println("Error running test scenario from file " + jsonFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }
}