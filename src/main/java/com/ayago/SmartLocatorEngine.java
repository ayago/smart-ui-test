package com.ayago;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class SmartLocatorEngine {
    private final WebDriver driver;
    
    @Autowired
    public SmartLocatorEngine(WebDriver driver){
        this.driver = driver;
    }
    
    public WebElement resolveField(String fieldName){
        try {
            WebElement label = driver.findElement(By.xpath("//label[normalize-space(text())='" + fieldName + "']"));
            String forId = label.getAttribute("for");
            if (forId != null)
                return driver.findElement(By.id(forId));
        } catch (NoSuchElementException ignored) {
        }
        
        try {
            String clean = fieldName.toLowerCase().replace(" ", "");
            return driver.findElement(By.xpath(
                "//input[@placeholder='" + fieldName + "' or @name='" + clean + "' or @id='" + clean + "']"));
        } catch (NoSuchElementException ignored) {
        }
        
        try {
            return driver.findElement(By.xpath(
                "//label[contains(text(),'" + fieldName + "')]/following-sibling::input"));
        } catch (NoSuchElementException ignored) {
        }
        
        // NEW: Try to match by title attribute
        try {
            return driver.findElement(By.xpath("//*[@" +
                "title='" + fieldName + "']"));
        } catch (NoSuchElementException ignored) {
        }
        
        throw new RuntimeException("Field not found: " + fieldName);
    }
    
    public void clickButton(String buttonText){
        try {
            WebElement button = driver.findElement(By.xpath("//button[normalize-space(text())='" + buttonText + "']"));
            button.click();
        } catch (NoSuchElementException e) {
            throw new RuntimeException("Action button not found: " + buttonText);
        }
    }
}
