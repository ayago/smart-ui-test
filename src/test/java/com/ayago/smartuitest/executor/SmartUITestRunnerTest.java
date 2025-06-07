package com.ayago.smartuitest.executor;

import com.ayago.smartuitest.engine.WebInteractionEngine;
import com.ayago.smartuitest.engine.WebInteractionEngineFactory;
import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.TestScenario;
import com.ayago.smartuitest.testscenario.TestScenario.ExpectedElement;
import com.ayago.smartuitest.testscenario.TestScenario.Page;
import com.ayago.smartuitest.testscenario.json.JsonTestScenarioParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartUITestRunnerTest {
    
    // Mock dependencies for SmartUITestRunner
    @Mock
    private JsonTestScenarioParser parser;
    @Mock
    private WebInteractionEngineFactory webInteractionEngineFactory;
    @Mock
    private FeatureManagerClient featureManager;
    @Mock
    private ExecutionPhotographer executionPhotographer;
    @Mock
    private RunnerProperties runnerProperties;
    @Mock
    private RunnerProperties.ScreenShot screenShot;
    
    // Inject mocks into the SmartUITestRunner instance
    private SmartUITestRunner smartUITestRunner;
    
    // A temporary directory for creating test JSON files
    private Path tempDirectory;
    
    @BeforeEach
    void setUp() throws IOException {
        // Configure the mock RunnerProperties to return a specific screenshot folder
        when(runnerProperties.getScreenShot()).thenReturn(screenShot);
        when(screenShot.getFolder()).thenReturn("target/screenshots");
        
        // Create a temporary directory for test files
        tempDirectory = Files.createTempDirectory("test_scenarios");
        smartUITestRunner = new SmartUITestRunner(
            parser,
            webInteractionEngineFactory,
            featureManager,
            executionPhotographer,
            runnerProperties
        );
    }
    

    @Test
    @DisplayName("Should print message and return if no arguments are provided")
    void run_noArguments_shouldPrintMessageAndReturn() throws Exception {
        // Call the run method with no arguments
        smartUITestRunner.run();
        
        // Verify that the parser and other interactions are never called
        verify(parser, never()).parse(any(File.class));
        verify(webInteractionEngineFactory, never()).create(any(WebDriver.class), anyString());
    }
    
    @Test
    @DisplayName("Should print message and return if directory path is invalid")
    void run_invalidDirectory_shouldPrintMessageAndReturn() throws Exception {
        // Call the run method with an invalid directory path
        smartUITestRunner.run("nonExistentDirectory");
        
        // Verify that the parser and other interactions are never called
        verify(parser, never()).parse(any(File.class));
        verify(webInteractionEngineFactory, never()).create(any(WebDriver.class), anyString());
    }
    
    @Test
    @DisplayName("Should print message and return if no JSON files are found in directory")
    void run_noJsonFilesInDirectory_shouldPrintMessageAndReturn() throws Exception {
        // Call the run method with a valid but empty temporary directory
        smartUITestRunner.run(tempDirectory.toString());
        
        // Verify that the parser and other interactions are never called
        verify(parser, never()).parse(any(File.class));
        verify(webInteractionEngineFactory, never()).create(any(WebDriver.class), anyString());
    }
    
    @Test
    @DisplayName("Should run single test scenario when a JSON file is found")
    void run_singleJsonFile_shouldRunScenario() throws Exception {
        // Create a dummy JSON file in the temporary directory
        Path jsonFilePath = tempDirectory.resolve("test_scenario.json");
        Files.writeString(jsonFilePath, "{ \"host\": \"http://localhost\" }"); // Minimal JSON content
        
        // Mock WebDriver, WebInteractionEngine, TestScenario, and pages
        WebDriver mockWebDriver = mock(WebDriver.class);
        WebInteractionEngine mockInteractionEngine = mock(WebInteractionEngine.class);
        TestScenario mockTestScenario = mock(TestScenario.class);
        Page mockPage = mock(Page.class);
        
        // Configure mocks for a successful scenario run
        when(webInteractionEngineFactory.create(any(WebDriver.class), anyString())).thenReturn(mockInteractionEngine);
        when(parser.parse(any(File.class))).thenReturn(mockTestScenario);
        when(mockTestScenario.getHost()).thenReturn("http://localhost");
        when(mockTestScenario.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario.getPages()).thenReturn(Collections.singletonList(mockPage));
        when(mockPage.getName()).thenReturn("HomePage");
        when(mockPage.getExpected()).thenReturn(Collections.emptyList()); // No expected elements for simplicity
        when(mockPage.getAction()).thenReturn(mock(Action.class));
        
        // Stub ChromeDriver constructor to return our mock WebDriver
        try (var mockedStatic = org.mockito.Mockito.mockStatic(ChromeDriver.class)) {
            mockedStatic.when(ChromeDriver::new).thenReturn(mockWebDriver);
            
            // Call the run method with the temporary directory
            smartUITestRunner.run(tempDirectory.toString());
        }
        
        
        // Verify interactions for a successful scenario run
        verify(parser, times(1)).parse(any(File.class));
        verify(webInteractionEngineFactory, times(1)).create(eq(mockWebDriver), eq("http://localhost"));
        verify(featureManager, times(1)).applyFeatureFlags(Collections.emptyMap());
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockWebDriver), eq("HomePage-On_Page"), eq(0), eq("target/screenshots"));
        verify(mockInteractionEngine, times(1)).performAction(any(Action.class), any(Runnable.class));
        verify(mockWebDriver, times(1)).quit();
    }
    
    @Test
    @DisplayName("Should run multiple test scenarios when multiple JSON files are found")
    void run_multipleJsonFiles_shouldRunMultipleScenarios() throws Exception {
        // Create multiple dummy JSON files
        Path jsonFilePath1 = tempDirectory.resolve("test_scenario1.json");
        Path jsonFilePath2 = tempDirectory.resolve("test_scenario2.json");
        Files.writeString(jsonFilePath1, "{ \"host\": \"http://localhost/1\" }");
        Files.writeString(jsonFilePath2, "{ \"host\": \"http://localhost/2\" }");
        
        // Mock WebDriver, WebInteractionEngine, TestScenario, and pages
        WebDriver mockWebDriver = mock(WebDriver.class); // This mock will be returned twice
        WebInteractionEngine mockInteractionEngine1 = mock(WebInteractionEngine.class);
        WebInteractionEngine mockInteractionEngine2 = mock(WebInteractionEngine.class);
        TestScenario mockTestScenario1 = mock(TestScenario.class);
        TestScenario mockTestScenario2 = mock(TestScenario.class);
        Page mockPage1 = mock(Page.class);
        Page mockPage2 = mock(Page.class);
        
        // Configure mocks for both scenarios
        when(webInteractionEngineFactory.create(any(WebDriver.class), eq("http://localhost/1"))).thenReturn(mockInteractionEngine1);
        when(webInteractionEngineFactory.create(any(WebDriver.class), eq("http://localhost/2"))).thenReturn(mockInteractionEngine2);
        when(parser.parse(jsonFilePath1.toFile())).thenReturn(mockTestScenario1);
        when(parser.parse(jsonFilePath2.toFile())).thenReturn(mockTestScenario2);
        
        when(mockTestScenario1.getHost()).thenReturn("http://localhost/1");
        when(mockTestScenario1.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario1.getPages()).thenReturn(Collections.singletonList(mockPage1));
        when(mockPage1.getName()).thenReturn("Scenario1Page");
        when(mockPage1.getExpected()).thenReturn(Collections.emptyList());
        when(mockPage1.getAction()).thenReturn(mock(Action.class));
        
        when(mockTestScenario2.getHost()).thenReturn("http://localhost/2");
        when(mockTestScenario2.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario2.getPages()).thenReturn(Collections.singletonList(mockPage2));
        when(mockPage2.getName()).thenReturn("Scenario2Page");
        when(mockPage2.getExpected()).thenReturn(Collections.emptyList());
        when(mockPage2.getAction()).thenReturn(mock(Action.class));
        
        // Stub ChromeDriver constructor to return our mock WebDriver
        try (var mockedStatic = org.mockito.Mockito.mockStatic(ChromeDriver.class)) {
            mockedStatic.when(ChromeDriver::new).thenReturn(mockWebDriver);
            
            // Call the run method
            smartUITestRunner.run(tempDirectory.toString());
        }
        
        // Verify interactions for both scenarios
        verify(parser, times(1)).parse(jsonFilePath1.toFile());
        verify(parser, times(1)).parse(jsonFilePath2.toFile());
        
        // WebDriver is created twice, so we verify its interactions twice
        verify(webInteractionEngineFactory, times(1)).create(eq(mockWebDriver), eq("http://localhost/1"));
        verify(webInteractionEngineFactory, times(1)).create(eq(mockWebDriver), eq("http://localhost/2"));
        
        verify(featureManager, times(2)).applyFeatureFlags(Collections.emptyMap());
        
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockWebDriver), eq("Scenario1Page-On_Page"), eq(0), eq("target/screenshots"));
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockWebDriver), eq("Scenario2Page-On_Page"), eq(0), eq("target/screenshots"));
        
        verify(mockInteractionEngine1, times(1)).performAction(any(Action.class), any(Runnable.class));
        verify(mockInteractionEngine2, times(1)).performAction(any(Action.class), any(Runnable.class));
        
        verify(mockWebDriver, times(2)).quit(); // quit is called twice
    }
    
    @Test
    @DisplayName("Should handle AssertionError when expected element value mismatch")
    void runTestScenario_expectedElementMismatch_shouldThrowAssertionError() throws Exception {
        // Create a dummy JSON file
        Path jsonFilePath = tempDirectory.resolve("mismatch_scenario.json");
        Files.writeString(jsonFilePath, "{ \"host\": \"http://localhost\" }");
        
        // Mock WebDriver, WebInteractionEngine, TestScenario, and pages
        WebDriver mockWebDriver = mock(WebDriver.class);
        WebInteractionEngine mockInteractionEngine = mock(WebInteractionEngine.class);
        TestScenario mockTestScenario = mock(TestScenario.class);
        Page mockPage = mock(Page.class);
        ExpectedElement mockExpectedElement = mock(ExpectedElement.class);
        
        // Configure mocks for a mismatch scenario
        when(webInteractionEngineFactory.create(any(WebDriver.class), anyString())).thenReturn(mockInteractionEngine);
        when(parser.parse(any(File.class))).thenReturn(mockTestScenario);
        when(mockTestScenario.getHost()).thenReturn("http://localhost");
        when(mockTestScenario.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario.getPages()).thenReturn(Collections.singletonList(mockPage));
        when(mockPage.getName()).thenReturn("MismatchPage");
        when(mockPage.getExpected()).thenReturn(Collections.singletonList(mockExpectedElement));
        when(mockExpectedElement.getTarget()).thenReturn("myField");
        when(mockExpectedElement.getValue()).thenReturn("expectedValue");
        when(mockInteractionEngine.getFieldValue(eq("myField"))).thenReturn("actualValue"); // Mismatch here!
        
        // Stub ChromeDriver constructor to return our mock WebDriver
        try (var mockedStatic = org.mockito.Mockito.mockStatic(ChromeDriver.class)) {
            mockedStatic.when(ChromeDriver::new).thenReturn(mockWebDriver);
            
            // Assert that an AssertionError is thrown
            AssertionError error = assertThrows(AssertionError.class, () ->
                smartUITestRunner.run(tempDirectory.toString())
            );
            
            // Verify the error message
            assertThat(error.getMessage(), containsString("Expected field 'myField' to be 'expectedValue' but found 'actualValue'"));
        }
        
        // Verify that quit() is still called in finally block
        verify(mockWebDriver, times(1)).quit();
        // Verify screenshot taken before assertion error
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockWebDriver), eq("MismatchPage-On_Page"), eq(0), eq("target/screenshots"));
        // Verify action is never performed due to assertion error
        verify(mockInteractionEngine, never()).performAction(any(Action.class), any(Runnable.class));
    }
    
    @Test
    @DisplayName("Should ensure webDriver.quit() is called even if an exception occurs during scenario execution")
    void runTestScenario_exceptionDuringExecution_shouldCallWebDriverQuit() throws Exception {
        // Create a dummy JSON file
        Path jsonFilePath = tempDirectory.resolve("exception_scenario.json");
        Files.writeString(jsonFilePath, "{ \"host\": \"http://localhost\" }");
        
        // Mock WebDriver, WebInteractionEngine, TestScenario
        WebDriver mockWebDriver = mock(WebDriver.class);
        WebInteractionEngine mockInteractionEngine = mock(WebInteractionEngine.class);
        TestScenario mockTestScenario = mock(TestScenario.class);
        
        // Configure mocks to throw an exception during parsing
        when(webInteractionEngineFactory.create(any(WebDriver.class), anyString())).thenReturn(mockInteractionEngine);
        doThrow(new RuntimeException("Simulated parsing error")).when(parser).parse(any(File.class));
        
        // Stub ChromeDriver constructor to return our mock WebDriver
        try (var mockedStatic = org.mockito.Mockito.mockStatic(ChromeDriver.class)) {
            mockedStatic.when(ChromeDriver::new).thenReturn(mockWebDriver);
            
            // Assert that the RuntimeException is rethrown by the run method
            RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                smartUITestRunner.run(tempDirectory.toString())
            );
            assertThat(thrown.getMessage(), is("Simulated parsing error"));
        }
        
        // Verify that quit() is called in the finally block
        verify(mockWebDriver, times(1)).quit();
        // Verify other interactions are not made if parsing fails
        verify(featureManager, never()).applyFeatureFlags(any());
        verify(executionPhotographer, never()).takeScreenshot(any(), anyString(), anyInt(), anyString());
        verify(mockInteractionEngine, never()).performAction(any(), any());
    }
    
    @Test
    @DisplayName("Should capture and execute the screenshot supplier for performAction")
    void runTestScenario_performAction_capturesAndExecutesScreenshotSupplier() throws Exception {
        // Create a dummy JSON file
        Path jsonFilePath = tempDirectory.resolve("action_screenshot_scenario.json");
        Files.writeString(jsonFilePath, "{ \"host\": \"http://localhost\" }");
        
        // Mock WebDriver, WebInteractionEngine, TestScenario, and pages
        WebDriver mockWebDriver = mock(WebDriver.class);
        WebInteractionEngine mockInteractionEngine = mock(WebInteractionEngine.class);
        TestScenario mockTestScenario = mock(TestScenario.class);
        Page mockPage = mock(Page.class);
        Action mockAction = mock(Action.class);
        
        // Configure mocks
        when(webInteractionEngineFactory.create(any(WebDriver.class), anyString())).thenReturn(mockInteractionEngine);
        when(parser.parse(any(File.class))).thenReturn(mockTestScenario);
        when(mockTestScenario.getHost()).thenReturn("http://localhost");
        when(mockTestScenario.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario.getPages()).thenReturn(Collections.singletonList(mockPage));
        when(mockPage.getName()).thenReturn("ActionPage");
        when(mockPage.getExpected()).thenReturn(Collections.emptyList());
        when(mockPage.getAction()).thenReturn(mockAction);
        
        // Capture the Supplier<Void> argument passed to performAction
        ArgumentCaptor<Runnable> screenshotSupplierCaptor = ArgumentCaptor.forClass(Runnable.class);
        
        // Configure performAction to not do anything initially, we will trigger the supplier manually
        doNothing().when(mockInteractionEngine).performAction(eq(mockAction), screenshotSupplierCaptor.capture());
        
        // Stub ChromeDriver constructor to return our mock WebDriver
        try (var mockedStatic = org.mockito.Mockito.mockStatic(ChromeDriver.class)) {
            mockedStatic.when(ChromeDriver::new).thenReturn(mockWebDriver);
            
            // Run the scenario
            smartUITestRunner.run(tempDirectory.toString());
        }
        
        // Verify initial screenshot before action
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockWebDriver), eq("ActionPage-On_Page"), eq(0), eq("target/screenshots"));
        
        // Verify performAction was called and capture the supplier
        Runnable runnableToBeExecuted = screenshotSupplierCaptor.getValue();
        verify(mockInteractionEngine, times(1)).performAction(eq(mockAction), runnableToBeExecuted);
        
        // Manually invoke the captured supplier
        runnableToBeExecuted.run();
        
        // Verify that executionPhotographer.takeScreenshot was called again by the supplier
        verify(executionPhotographer, times(2)).takeScreenshot(eq(mockWebDriver), eq("ActionPage-On_Page"), eq(0), eq("target/screenshots"));
        
        verify(mockWebDriver, times(1)).quit();
    }
    
    @Test
    @DisplayName("Should handle scenario with multiple pages and expected elements")
    void runTestScenario_multiplePagesAndExpectedElements_shouldProcessCorrectly() throws Exception {
        // Create a dummy JSON file
        Path jsonFilePath = tempDirectory.resolve("multi_page_scenario.json");
        Files.writeString(jsonFilePath, "{ \"host\": \"http://localhost\" }");
        
        // Mock WebDriver, WebInteractionEngine, TestScenario, and pages
        WebDriver mockWebDriver = mock(WebDriver.class);
        WebInteractionEngine mockInteractionEngine = mock(WebInteractionEngine.class);
        TestScenario mockTestScenario = mock(TestScenario.class);
        
        Page page1 = mock(Page.class);
        Page page2 = mock(Page.class);
        
        ExpectedElement expected1_1 = mock(ExpectedElement.class);
        ExpectedElement expected1_2 = mock(ExpectedElement.class);
        ExpectedElement expected2_1 = mock(ExpectedElement.class);
        
        Action action1 = mock(Action.class);
        Action action2 = mock(Action.class);
        
        // Configure overall scenario
        when(webInteractionEngineFactory.create(any(WebDriver.class), anyString())).thenReturn(mockInteractionEngine);
        when(parser.parse(any(File.class))).thenReturn(mockTestScenario);
        when(mockTestScenario.getHost()).thenReturn("http://localhost");
        when(mockTestScenario.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario.getPages()).thenReturn(Arrays.asList(page1, page2));
        
        // Configure Page 1
        when(page1.getName()).thenReturn("PageOne");
        when(page1.getExpected()).thenReturn(Arrays.asList(expected1_1, expected1_2));
        when(expected1_1.getTarget()).thenReturn("field1");
        when(expected1_1.getValue()).thenReturn("value1");
        when(expected1_2.getTarget()).thenReturn("field2");
        when(expected1_2.getValue()).thenReturn("value2");
        when(mockInteractionEngine.getFieldValue("field1")).thenReturn("value1");
        when(mockInteractionEngine.getFieldValue("field2")).thenReturn("value2");
        when(page1.getAction()).thenReturn(action1);
        
        // Configure Page 2
        when(page2.getName()).thenReturn("PageTwo");
        when(page2.getExpected()).thenReturn(Collections.singletonList(expected2_1));
        when(expected2_1.getTarget()).thenReturn("field3");
        when(expected2_1.getValue()).thenReturn("value3");
        when(mockInteractionEngine.getFieldValue("field3")).thenReturn("value3");
        when(page2.getAction()).thenReturn(action2);
        
        // Stub ChromeDriver constructor to return our mock WebDriver
        try (var mockedStatic = org.mockito.Mockito.mockStatic(ChromeDriver.class)) {
            mockedStatic.when(ChromeDriver::new).thenReturn(mockWebDriver);
            
            // Run the scenario
            smartUITestRunner.run(tempDirectory.toString());
        }
        
        // Verify interactions
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockWebDriver), eq("PageOne-On_Page"), eq(0), eq("target/screenshots"));
        verify(mockInteractionEngine, times(1)).getFieldValue("field1");
        verify(mockInteractionEngine, times(1)).getFieldValue("field2");
        verify(mockInteractionEngine, times(1)).performAction(eq(action1), any(Runnable.class));
        
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockWebDriver), eq("PageTwo-On_Page"), eq(1), eq("target/screenshots"));
        verify(mockInteractionEngine, times(1)).getFieldValue("field3");
        verify(mockInteractionEngine, times(1)).performAction(eq(action2), any(Runnable.class));
        
        verify(mockWebDriver, times(1)).quit(); // quit called once at the end
    }
    
    @Test
    @DisplayName("Should handle an IOException during file walking gracefully")
    void run_ioExceptionDuringWalk_shouldThrowRuntimeException() throws Exception {
        // Define the path string for the mocked scenario
        String testDirectoryPath = "/mocked/io-exception/directory";
        
        // Mock the static Files and Paths methods, and use mockConstruction for File
        try (
           
            var mockedStaticFiles = org.mockito.Mockito.mockStatic(Files.class);
            var mockedStaticPaths = org.mockito.Mockito.mockStatic(Paths.class)
        ) {
            // Mock Paths.get for the specific test directory path
            Path mockedDirectoryPath = mock(Path.class);
            mockedStaticPaths.when(() -> Paths.get(eq(testDirectoryPath))).thenReturn(mockedDirectoryPath);
            
            // Simulate Files.walk throwing an IOException
            mockedStaticFiles.when(() -> Files.walk(eq(mockedDirectoryPath)))
                .thenThrow(new IOException("Simulated file walk error"));
            
            // Expect a RuntimeException to be thrown
            IOException thrown = assertThrows(IOException.class, () ->{
                    var mockedConstructedFile = org.mockito.Mockito.mockConstruction(File.class, (mock, context) -> {
                        // Intercept the constructor call to new File(testDirectoryPath)
                        if (context.arguments().get(0).equals(testDirectoryPath)) {
                            when(mock.exists()).thenReturn(true);
                            when(mock.isDirectory()).thenReturn(true);
                        } else {
                            // Default behavior for other File constructions if any, though not expected in this test
                            when(mock.exists()).thenReturn(false);
                            when(mock.isDirectory()).thenReturn(false);
                        }
                    });
                
                    smartUITestRunner.run(testDirectoryPath);
                }
                
            );
            
            assertThat(thrown.getMessage(), containsString("Simulated file walk error"));
        }
        
        // Verify no other interactions
        verify(parser, never()).parse(any(File.class));
    }
}
