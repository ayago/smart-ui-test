package com.ayago.smartuitest.executor;

import com.ayago.smartuitest.engine.WebInteractionEngine;
import com.ayago.smartuitest.engine.WebInteractionEngineFactory;
import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.TestScenario;
import com.ayago.smartuitest.testscenario.TestScenario.ExpectedElement;
import com.ayago.smartuitest.testscenario.TestScenario.Page;
import com.ayago.smartuitest.testscenario.json.JsonTestScenarioParser;
import org.junit.jupiter.api.AfterEach;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartUITestRunnerTest {
    
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
    
    private SmartUITestRunner smartUITestRunner;
    
    private Path tempTestDir; // Use Path for easier file system operations
    
    @BeforeEach
    void setUp() throws IOException {
        // Configure the mock RunnerProperties to return a specific screenshot folder
        when(runnerProperties.getScreenShot()).thenReturn(screenShot);
        when(screenShot.getFolder()).thenReturn("target/screenshots");
        
        // Create a temporary directory for test files
        tempTestDir = Files.createTempDirectory("smart_ui_test_runner_tests");
        smartUITestRunner = new SmartUITestRunner(
            parser,
            webInteractionEngineFactory,
            featureManager,
            executionPhotographer,
            runnerProperties
        );
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up the temporary directory and its contents
        if (tempTestDir != null) {
            Files.walk(tempTestDir)
                .sorted(Comparator.reverseOrder()) // Sort in reverse order for child-first deletion
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
    
    @Test
    @DisplayName("Should print message and return if no arguments are provided")
    void run_noArguments_shouldPrintMessageAndReturn() throws Exception {
        smartUITestRunner.run();
        
        verify(parser, never()).parse(any(File.class));
        verify(webInteractionEngineFactory, never()).create(any(WebDriver.class), anyString());
        // We don't verify File.exists() or isDirectory() because the method should return early
        // before even attempting to create a File object for a null/empty path argument.
    }
    
    @Test
    @DisplayName("Should print message and return if directory path is invalid (non-existent)")
    void run_invalidDirectory_nonExistent_shouldPrintMessageAndReturn() throws Exception {
        String nonExistentPath = tempTestDir.resolve("non_existent_dir").toString();
        
        smartUITestRunner.run(nonExistentPath);
        
        // Verify no interactions with parser or webInteractionEngineFactory
        verify(parser, never()).parse(any(File.class));
        verify(webInteractionEngineFactory, never()).create(any(WebDriver.class), anyString());
    }
    
    @Test
    @DisplayName("Should print message and return if directory path is invalid (not a directory)")
    void run_invalidDirectory_notADirectory_shouldPrintMessageAndReturn() throws Exception {
        // Create a file, not a directory
        Path filePath = tempTestDir.resolve("not_a_directory.txt");
        Files.writeString(filePath, "dummy content");
        
        smartUITestRunner.run(filePath.toString());
        
        // Verify no interactions with parser or webInteractionEngineFactory
        verify(parser, never()).parse(any(File.class));
        verify(webInteractionEngineFactory, never()).create(any(WebDriver.class), anyString());
    }
    
    @Test
    @DisplayName("Should print message and return if no JSON files are found in directory")
    void run_noJsonFilesInDirectory_shouldPrintMessageAndReturn() throws Exception {
        // tempTestDir is already created and empty by default in @BeforeEach
        
        smartUITestRunner.run(tempTestDir.toString());
        
        // Verify no interactions with parser or webInteractionEngineFactory
        verify(parser, never()).parse(any(File.class));
        verify(webInteractionEngineFactory, never()).create(any(WebDriver.class), anyString());
    }
    
    @Test
    @DisplayName("Should run single test scenario when a JSON file is found")
    void run_singleJsonFile_shouldRunScenario() throws Exception {
        // Create a dummy JSON file in the temporary directory
        Path jsonFilePath = tempTestDir.resolve("test_scenario.json");
        Files.writeString(jsonFilePath, "{ \"host\": \"http://localhost\" }"); // Minimal JSON content
        
        // Mock WebDriver, WebInteractionEngine, TestScenario, and pages
        WebDriver mockWebDriver = mock(ChromeDriver.class);
        WebInteractionEngine mockInteractionEngine = mock(WebInteractionEngine.class);
        TestScenario mockTestScenario = mock(TestScenario.class);
        Page mockPage = mock(Page.class);
        
        // Configure mocks for a successful scenario run
        when(webInteractionEngineFactory.create(any(WebDriver.class), anyString())).thenReturn(mockInteractionEngine);
        when(parser.parse(any(File.class))).thenReturn(mockTestScenario); // Use real File object
        when(mockTestScenario.getHost()).thenReturn("http://localhost");
        when(mockTestScenario.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario.getPages()).thenReturn(Collections.singletonList(mockPage));
        when(mockPage.getName()).thenReturn("HomePage");
        when(mockPage.getExpected()).thenReturn(Collections.emptyList());
        when(mockPage.getAction()).thenReturn(mock(Action.class));
        
        // Stub ChromeDriver constructor to return our mock WebDriver
        try (var mockedStaticChromeDriver = org.mockito.Mockito.mockConstruction(ChromeDriver.class)) {
            
            
            // Call the run method with the temporary directory
            smartUITestRunner.run(tempTestDir.toString());
            mockWebDriver = mockedStaticChromeDriver.constructed().getFirst();
        }
        
        // Verify interactions for a successful scenario run
        verify(parser, times(1)).parse(eq(jsonFilePath.toFile()));
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
        Path jsonFilePath1 = tempTestDir.resolve("test_scenario1.json");
        Path jsonFilePath2 = tempTestDir.resolve("test_scenario2.json");
        Files.writeString(jsonFilePath1, "{ \"host\": \"http://localhost/1\" }");
        Files.writeString(jsonFilePath2, "{ \"host\": \"http://localhost/2\" }");
        
        // Mock WebDriver, WebInteractionEngine, TestScenario, and pages
        Page mockPage1 = mock(Page.class);
        when(mockPage1.getName()).thenReturn("Scenario1Page");
        when(mockPage1.getExpected()).thenReturn(Collections.emptyList());
        when(mockPage1.getAction()).thenReturn(mock(Action.class));
        
        TestScenario mockTestScenario1 = mock(TestScenario.class);
        when(mockTestScenario1.getHost()).thenReturn("http://localhost/1");
        when(mockTestScenario1.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario1.getPages()).thenReturn(Collections.singletonList(mockPage1));
        
        when(parser.parse(argThat(file -> file != null && file.toString().equals(jsonFilePath1.toString())))).thenReturn(mockTestScenario1);
        
        Page mockPage2 = mock(Page.class);
        when(mockPage2.getName()).thenReturn("Scenario2Page");
        when(mockPage2.getExpected()).thenReturn(Collections.emptyList());
        when(mockPage2.getAction()).thenReturn(mock(Action.class));
        
        TestScenario mockTestScenario2 = mock(TestScenario.class);
        when(mockTestScenario2.getHost()).thenReturn("http://localhost/2");
        when(mockTestScenario2.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario2.getPages()).thenReturn(Collections.singletonList(mockPage2));
        
        when(parser.parse(argThat(file -> file != null && file.toString().equals(jsonFilePath2.toString())))).thenReturn(mockTestScenario2);
        
        // Configure mocks for both scenarios
        WebInteractionEngine mockInteractionEngine1 = mock(WebInteractionEngine.class);
        when(webInteractionEngineFactory.create(any(WebDriver.class), eq("http://localhost/1"))).thenReturn(mockInteractionEngine1);
        
        WebInteractionEngine mockInteractionEngine2 = mock(WebInteractionEngine.class);
        when(webInteractionEngineFactory.create(any(WebDriver.class), eq("http://localhost/2"))).thenReturn(mockInteractionEngine2);
        

        try (var mockedChromeDriverConstruction = org.mockito.Mockito.mockConstruction(ChromeDriver.class)) {
            // Call the run method
            smartUITestRunner.run(tempTestDir.toString());
        }
        
        // Verify interactions for both scenarios
        verify(parser, times(1)).parse(eq(jsonFilePath1.toFile()));
        verify(parser, times(1)).parse(eq(jsonFilePath2.toFile()));
        
        ArgumentCaptor<WebDriver> scenarioOneDriverCaptor = ArgumentCaptor.forClass(WebDriver.class);
        verify(webInteractionEngineFactory, times(1)).create(scenarioOneDriverCaptor.capture(), eq("http://localhost/1"));
        WebDriver mockDriverOne = scenarioOneDriverCaptor.getValue();
        assertNotNull(mockDriverOne);
        
        ArgumentCaptor<WebDriver> scenarioTwoDriverCaptor = ArgumentCaptor.forClass(WebDriver.class);
        verify(webInteractionEngineFactory, times(1)).create(scenarioTwoDriverCaptor.capture(), eq("http://localhost/2"));
        WebDriver mockDriverTwo = scenarioTwoDriverCaptor.getValue();
        assertNotNull(mockDriverTwo);
        
        verify(featureManager, times(2)).applyFeatureFlags(Collections.emptyMap());
        
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockDriverOne), eq("Scenario1Page-On_Page"), eq(0), eq("target/screenshots"));
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockDriverTwo), eq("Scenario2Page-On_Page"), eq(0), eq("target/screenshots"));
        
        verify(mockInteractionEngine1, times(1)).performAction(any(Action.class), any(Runnable.class));
        verify(mockInteractionEngine2, times(1)).performAction(any(Action.class), any(Runnable.class));
        
        verify(mockDriverOne, times(1)).quit();
        verify(mockDriverTwo, times(1)).quit();
    }
    
    @Test
    @DisplayName("Should handle AssertionError when expected element value mismatch")
    void runTestScenario_expectedElementMismatch_shouldThrowAssertionError() throws Exception {
        // Create a dummy JSON file
        Path jsonFilePath = tempTestDir.resolve("mismatch_scenario.json");
        Files.writeString(jsonFilePath, "{ \"host\": \"http://localhost\" }");
        
        // Mock WebDriver, WebInteractionEngine, TestScenario, and pages
        WebDriver mockWebDriver;
        WebInteractionEngine mockInteractionEngine = mock(WebInteractionEngine.class);
        TestScenario mockTestScenario = mock(TestScenario.class);
        Page mockPage = mock(Page.class);
        ExpectedElement mockExpectedElement = mock(ExpectedElement.class);
        
        // Configure mocks for a mismatch scenario
        when(webInteractionEngineFactory.create(any(WebDriver.class), anyString())).thenReturn(mockInteractionEngine);
        when(parser.parse(eq(jsonFilePath.toFile()))).thenReturn(mockTestScenario);
        when(mockTestScenario.getHost()).thenReturn("http://localhost");
        when(mockTestScenario.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario.getPages()).thenReturn(Collections.singletonList(mockPage));
        when(mockPage.getName()).thenReturn("MismatchPage");
        when(mockPage.getExpected()).thenReturn(Collections.singletonList(mockExpectedElement));
        when(mockExpectedElement.getTarget()).thenReturn("myField");
        when(mockExpectedElement.getValue()).thenReturn("expectedValue");
        when(mockInteractionEngine.getFieldValue(eq("myField"))).thenReturn("actualValue"); // Mismatch here!
        
        // Stub ChromeDriver constructor to return our mock WebDriver
        try (var mockedConstruction = org.mockito.Mockito.mockConstruction(ChromeDriver.class)) {
            
            // Assert that an AssertionError is thrown
            AssertionError error = assertThrows(AssertionError.class, () ->
                smartUITestRunner.run(tempTestDir.toString())
            );
            
            // Verify the error message
            assertThat(error.getMessage(), containsString("Expected field 'myField' to be 'expectedValue' but found 'actualValue'"));
            mockWebDriver = mockedConstruction.constructed().getFirst();
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
        Path jsonFilePath = tempTestDir.resolve("exception_scenario.json");
        Files.writeString(jsonFilePath, "{ \"host\": \"http://localhost\" }");
        
        // Mock WebDriver, WebInteractionEngine, TestScenario
        WebDriver mockWebDriver;
        WebInteractionEngine mockInteractionEngine = mock(WebInteractionEngine.class);
        
        // Configure mocks to throw an exception during parsing
        when(parser.parse(any(File.class)))
            .thenThrow(new RuntimeException("Simulated parsing error"));
        
        // Stub ChromeDriver constructor to return our mock WebDriver
        try (var mockedConstruction = org.mockito.Mockito.mockConstruction(ChromeDriver.class)) {
            // Assert that the RuntimeException is rethrown by the run method
            RuntimeException capturedException = assertThrows(RuntimeException.class, () -> smartUITestRunner.run(tempTestDir.toString()));
            assertEquals("Simulated parsing error", capturedException.getMessage());
            mockWebDriver = mockedConstruction.constructed().getFirst();
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
        Path jsonFilePath = tempTestDir.resolve("action_screenshot_scenario.json");
        Files.writeString(jsonFilePath, "{ \"host\": \"http://localhost\" }");
        
        // Mock WebDriver, WebInteractionEngine, TestScenario, and pages
        WebDriver mockWebDriver;
        WebInteractionEngine mockInteractionEngine = mock(WebInteractionEngine.class);
        TestScenario mockTestScenario = mock(TestScenario.class);
        Page mockPage = mock(Page.class);
        Action mockAction = mock(Action.class);
        
        // Configure mocks
        when(webInteractionEngineFactory.create(any(WebDriver.class), anyString())).thenReturn(mockInteractionEngine);
        when(parser.parse(eq(jsonFilePath.toFile()))).thenReturn(mockTestScenario);
        when(mockTestScenario.getHost()).thenReturn("http://localhost");
        when(mockTestScenario.getFeatures()).thenReturn(Collections.emptyMap());
        when(mockTestScenario.getPages()).thenReturn(Collections.singletonList(mockPage));
        when(mockPage.getName()).thenReturn("ActionPage");
        when(mockPage.getExpected()).thenReturn(Collections.emptyList());
        when(mockPage.getAction()).thenReturn(mockAction);
        
        // Capture the Runnable argument passed to performAction
        ArgumentCaptor<Runnable> screenshotSupplierCaptor = ArgumentCaptor.forClass(Runnable.class);
        
        // Configure performAction to not do anything initially, we will trigger the supplier manually
        doNothing().when(mockInteractionEngine).performAction(eq(mockAction), screenshotSupplierCaptor.capture());
        
        // Stub ChromeDriver constructor to return our mock WebDriver
        try (var mockedConstruction = org.mockito.Mockito.mockConstruction(ChromeDriver.class)) {
            // Run the scenario
            smartUITestRunner.run(tempTestDir.toString());
            mockWebDriver = mockedConstruction.constructed().getFirst();
        }
        
        // Verify initial screenshot before action
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockWebDriver), eq("ActionPage-On_Page"), eq(0), eq("target/screenshots"));
        
        // Verify performAction was called and capture the supplier
        verify(mockInteractionEngine, times(1)).performAction(eq(mockAction), any(Runnable.class));
        Runnable capturedSupplier = screenshotSupplierCaptor.getValue();
        
        // Manually invoke the captured supplier
        capturedSupplier.run();
        
        // Verify that executionPhotographer.takeScreenshot was called again by the supplier
        verify(executionPhotographer, times(2)).takeScreenshot(eq(mockWebDriver), eq("ActionPage-On_Page"), eq(0), eq("target/screenshots"));
        
        verify(mockWebDriver, times(1)).quit();
    }
    
    @Test
    @DisplayName("Should handle scenario with multiple pages and expected elements")
    void runTestScenario_multiplePagesAndExpectedElements_shouldProcessCorrectly() throws Exception {
        // Create a dummy JSON file
        Path jsonFilePath = tempTestDir.resolve("multi_page_scenario.json");
        Files.writeString(jsonFilePath, "{ \"host\": \"http://localhost\" }");
        
        // Mock WebDriver, WebInteractionEngine, TestScenario, and pages
        WebDriver mockWebDriver;
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
        when(parser.parse(eq(jsonFilePath.toFile()))).thenReturn(mockTestScenario);
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
        try (var mockedConstruction = org.mockito.Mockito.mockConstruction(ChromeDriver.class)) {
            // Run the scenario
            smartUITestRunner.run(tempTestDir.toString());
            mockWebDriver = mockedConstruction.constructed().getFirst();
        }
        
        // Verify interactions
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockWebDriver), eq("PageOne-On_Page"), eq(0), eq("target/screenshots"));
        verify(mockInteractionEngine, times(1)).getFieldValue("field1");
        verify(mockInteractionEngine, times(1)).getFieldValue("field2");
        verify(mockInteractionEngine, times(1)).performAction(eq(action1), any(Runnable.class));
        
        verify(executionPhotographer, times(1)).takeScreenshot(eq(mockWebDriver), eq("PageTwo-On_Page"), eq(1), eq("target/screenshots"));
        verify(mockInteractionEngine, times(1)).getFieldValue("field3");
        verify(mockInteractionEngine, times(1)).performAction(eq(action2), any(Runnable.class));
        
        verify(mockWebDriver, times(1)).quit();
    }
}
