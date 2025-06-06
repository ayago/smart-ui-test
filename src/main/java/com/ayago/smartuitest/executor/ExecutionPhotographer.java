package com.ayago.smartuitest.executor;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.io.FileHandler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class ExecutionPhotographer{
    
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
