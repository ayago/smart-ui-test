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
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.io.FileHandler;
import org.springframework.beans.factory.annotation.Value; // Added import for @Value
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    
    /**
     * Takes a screenshot of the current WebDriver instance and saves it.
     * Screenshots are stored in the specified base directory.
     * Filenames are generated based on the scenario name, page number, and a timestamp.
     *
     * @param driver The WebDriver instance.
     * @param scenarioName The name of the test scenario.
     * @param pageNumber The current page number within the scenario.
     * @param baseDir The base directory where screenshots should be saved.
     */
    public void takeScreenshot(WebDriver driver, String scenarioName, int pageNumber, String baseDir) {
        // Create the specified base directory if it doesn't exist
        File screenshotsDir = new File(baseDir);
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs();
        }
        
        // Sanitize scenarioName for filename (replace spaces and special chars)
        String sanitizedScenarioName = scenarioName.replaceAll("[^a-zA-Z0-9.-]", "_");
        
        // Generate a timestamp for unique filenames
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        
        // Construct the filename: scenarioName_pageX_timestamp.png
        String fileName = String.format("%s_page%d_%s.png", sanitizedScenarioName, pageNumber, timestamp);
        String fullPath = screenshotsDir.getAbsolutePath() + File.separator + fileName;
        
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            File sourceFile = ts.getScreenshotAs(OutputType.FILE);
            File destinationFile = new File(fullPath);
            FileHandler.copy(sourceFile, destinationFile);
            System.out.println("Screenshot saved to: " + destinationFile.getAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("Failed to save screenshot: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassCastException e) {
            System.err.println("WebDriver cannot be cast to TakesScreenshot. This driver does not support screenshots.");
            e.printStackTrace();
        }
    }
}