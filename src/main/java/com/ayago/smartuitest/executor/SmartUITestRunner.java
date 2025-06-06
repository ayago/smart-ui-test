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
import org.springframework.beans.factory.annotation.Value; // Added import for @Value
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

import static java.nio.file.Files.walk;

@Component
class SmartUITestRunner implements CommandLineRunner {
    private final JsonTestScenarioParser parser;
    private final WebInteractionEngineFactory webInteractionEngineFactory;
    private final FeatureManagerClient featureManager;
    private final ExecutionPhotographer executionPhotographer;
    
    // Inject the screenshot folder from application.yaml using @Value
    @Value("${screenshot.folder}")
    private String screenshotsBaseDir;
    
    public SmartUITestRunner(
        JsonTestScenarioParser parser,
        WebInteractionEngineFactory webInteractionEngineFactory,
        FeatureManagerClient featureManager,
        ExecutionPhotographer executionPhotographer
        // Assuming FeatureManagerClient exists
    ) {
        this.parser = parser;
        this.webInteractionEngineFactory = webInteractionEngineFactory;
        this.featureManager = featureManager; // Assuming FeatureManagerClient exists
        this.executionPhotographer = executionPhotographer;
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Screenshots will be saved to: " + new File(screenshotsBaseDir).getAbsolutePath());
        
        String directoryPath = getDirectoryPath(args);
        if (directoryPath == null) return; // Exit if path is invalid
        
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
            System.err.println("An error occurred during test execution: " + e.getMessage());
            throw e;
        }
    }
    
    private String getDirectoryPath(String[] args){
        if (args.length == 0) {
            System.out.println("No directory path argument provided.");
            return null;
        }
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
            
            IntStream.range(0, definition.getPages().size())
                .forEach(pageCounter -> {
                    Page page = definition.getPages().get(pageCounter);
                    
                    executionPhotographer.takeScreenshot(webDriver, page.getName()+"-On_Page", pageCounter, screenshotsBaseDir);
                    
                    for (ExpectedElement expected : page.getExpected()) {
                        String actualValue = interactionEngine.getFieldValue(expected.getTarget());
                        if (!actualValue.equals(expected.getValue())) {
                            throw new AssertionError(
                                "Expected field '" + expected.getTarget() + "' to be '" + expected.getValue() + "' but found '" +
                                    actualValue + "'");
                        }
                    }
                    
                    Action action = page.getAction();
                    
                    interactionEngine.performAction(
                        action,
                        () -> executionPhotographer.takeScreenshot(webDriver, page.getName()+"-On_Page", pageCounter, screenshotsBaseDir)
                    );
                });
            
            
        } catch (Exception e) {
            System.err.println("Error running test scenario from file " + jsonFile.getAbsolutePath() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            webDriver.quit();
        }
    }
    
}