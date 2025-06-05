package com.ayago.smartuitest.executor;

import com.ayago.smartuitest.engine.WebInteractionEngine;
import com.ayago.smartuitest.engine.WebInteractionEngineFactory;
import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.TestScenario;
import com.ayago.smartuitest.testscenario.TestScenario.ExpectedElement;
import com.ayago.smartuitest.testscenario.TestScenario.Page;
import com.ayago.smartuitest.testscenario.json.JsonTestScenarioParser;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
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
    private final WebInteractionEngineFactory webInteractionEngineFactory;
    private final FeatureManagerClient featureManager; // Assuming FeatureManagerClient exists
    
    public SmartUITestRunner(
        JsonTestScenarioParser parser,
        WebInteractionEngineFactory webInteractionEngineFactory,
        FeatureManagerClient featureManager // Assuming FeatureManagerClient exists
    ) {
        this.parser = parser;
        this.webInteractionEngineFactory = webInteractionEngineFactory;
        this.featureManager = featureManager; // Assuming FeatureManagerClient exists
    }
    
    @Override
    public void run(String... args) throws Exception {
        String directoryPath = getDirectoryPath(args);
        if (directoryPath == null) return; // Exit if path is invalid
        
        // Wrap the main execution logic in a try-finally block
        try {
            if (args.length == 0) {
                System.out.println("Please provide the directory path containing JSON test scenario files as an argument.");
                return; // Exit if no argument is provided
            }
            
            List<File> jsonFiles = walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .map(Path::toFile)
                .toList();
            
            if (jsonFiles.isEmpty()) {
                System.out.println("No JSON files found in the specified directory: " + directoryPath);
                return; // Exit if no JSON files are found
            }
            
            
            for (File jsonFile : jsonFiles) {
                System.out.println("Running test scenario from file: " + jsonFile.getAbsolutePath());
                runTestScenario(jsonFile); // Delegate to the private method
            }
            
        } catch (Exception e) {
            // Catch any exceptions that might occur outside the runTestScenario method
            System.err.println("An error occurred during test execution: " + e.getMessage());
            throw e; // Re-throw the exception if you want the application to indicate failure
        }
    }
    
    private String getDirectoryPath(String[] args){
        String directoryPath = args[0];
        File directory = new File(directoryPath);
        
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Invalid directory path provided: " + directoryPath);
            return null;
        }
        return directoryPath;
    }
    
    /**
     * Runs a single test scenario from a JSON file.
     *
     * @param jsonFile The JSON file containing the test scenario definition.
     */
    private void runTestScenario(File jsonFile) {
        WebDriver webDriver = new ChromeDriver();
        try {
            TestScenario definition = parser.parse(jsonFile);
            
            WebInteractionEngine interactionEngine = webInteractionEngineFactory.create(webDriver, definition.getHost());
            System.out.println("Target Host: " + definition.getHost());
            featureManager.applyFeatureFlags(definition.getFeatures()); // Assuming applyFeatureFlags exists
            
            for (Page page : definition.getPages()) {
                
                for (ExpectedElement expected : page.getExpected()) {
                    String actualValue = interactionEngine.getFieldValue(expected.getTarget());
                    if (!actualValue.equals(expected.getValue())) {
                        throw new AssertionError(
                            "Expected field '" + expected.getTarget() + "' to be '" + expected.getValue() + "' but found '" +
                                actualValue + "'");
                    }
                }
                
                Action action = page.getAction();
                // Assuming performAction in WebInteractionEngine now accepts WebDriver
                // as discussed in the previous turn for EnterActionStrategy
                interactionEngine.performAction(action);
            }
            
        } catch (Exception e) {
            // Catch and report errors for individual test scenario files
            System.err.println("Error running test scenario from file " + jsonFile.getAbsolutePath() + ": " + e.getMessage());
            e.printStackTrace(); // Print stack trace for the specific file error
            // Do NOT re-throw here if you want the runner to continue with other files
            // If you re-throw here, the outer finally block will still execute driver.quit()
        } finally {
            webDriver.quit();
        }
    }
}
