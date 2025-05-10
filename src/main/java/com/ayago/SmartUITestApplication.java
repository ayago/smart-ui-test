package com.ayago;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SmartUITestApplication{
    public static void main(String[] args) {
        SpringApplication.run(SmartUITestApplication.class, args);
    }
    
    @Bean
    public WebDriver webDriver() {
        return new ChromeDriver();
    }
}
