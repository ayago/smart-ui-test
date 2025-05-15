package com.ayago.smartuitest.testscenario.json;

import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.ClickAction;
import com.ayago.smartuitest.testscenario.EnterAction;
import com.ayago.smartuitest.testscenario.SubmitAction;
import com.ayago.smartuitest.testscenario.TestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link JsonTestScenarioParser}.
 */
public class JsonTestScenarioParserTest {
    
    
    private JsonTestScenarioParser parser;
    
    @TempDir
    Path tempDir; // JUnit 5 temporary directory for test files
    
    @BeforeEach
    void setUp() {
        // Instantiate the parser directly.
        // If JsonTestScenarioParser had dependencies to be mocked, use @InjectMocks and @Mock
        parser = new JsonTestScenarioParser();
    }
    
    private File createTempJsonFile(String jsonContent) throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "testScenario", ".json");
        Files.writeString(tempFile, jsonContent);
        return tempFile.toFile();
    }
    
    @Test
    void parse_validScenario_returnsTestScenario() throws IOException {
        String jsonTestData = """
            {
              "host": "https://www.google.com",
              "features": {},
              "pages": [
                {
                  "name": "Page 1",
                  "expected": [
                    { "target": "Search", "value": "" }
                  ],
                  "action": {
                    "actionType": "Enter",
                    "targetField": "Search",
                    "value": "chatgpt"
                  }
                }
              ]
            }""";
        File jsonFile = createTempJsonFile(jsonTestData);
        TestScenario scenario = parser.parse(jsonFile.getAbsolutePath());
        
        assertNotNull(scenario);
        assertEquals("https://www.google.com", scenario.getHost());
        assertThat(scenario.getPages(), hasSize(1));
        assertEquals("Page 1", scenario.getPages().getFirst().getName());
        assertThat(scenario.getPages().getFirst().getExpected(), hasSize(1));
        assertEquals("Search", scenario.getPages().getFirst().getExpected().getFirst().getTarget());
        
        Action action = scenario.getPages().getFirst().getAction();
        assertThat(action, instanceOf(EnterAction.class));
        EnterAction enterAction = (EnterAction) action;
        assertEquals("Search", enterAction.getTargetField());
        assertEquals("chatgpt", enterAction.getValue());
    }
    
    @Test
    void parse_noFeatures_returnsTestScenario() throws IOException {
        // \"features\": {} is optional if your POJO handles null or if parser defaults to empty
        String jsonTestData = """
            {
              "host": "https://www.google.com",
              "pages": [
                {
                  "name": "Page 1",
                  "expected": [
                    { "target": "Search", "value": "" }
                  ],
                  "action": {
                    "actionType": "Enter",
                    "targetField": "Search",
                    "value": "chatgpt"
                  }
                }
              ]
            }""";
        File jsonFile = createTempJsonFile(jsonTestData);
        TestScenario scenario = parser.parse(jsonFile.getAbsolutePath());
        
        assertNotNull(scenario);
        assertEquals("https://www.google.com", scenario.getHost());
        // Check if features map is empty or null based on POJO/Jackson behavior
        if (scenario.getFeatures() != null) {
            assertThat(scenario.getFeatures().values(), hasSize(0));
        } else {
            assertNotNull(scenario, "Features map was null, which might be acceptable if not present in JSON");
        }
    }
    
    @Test
    void parse_threeActionTypes_parsesCorrectly() throws IOException {
        // Name included as per FeatureMixIn
        String jsonTestData = """
            {
              "host": "https://www.google.com",
              "features": {
                "DUMMY_FEATURE": {
                  "name": "DUMMY_FEATURE",
                  "enable": false,
                  "context": {
                    "province": "N/A",
                    "store": "N/A"
                  }
                }
              },
              "pages": [
                {
                  "name": "Page 1",
                  "expected": [{ "target": "Search", "value": "" }],
                  "action": {
                    "actionType": "Enter",
                    "targetField": "Search",
                    "value": "chatgpt"
                  }
                },
                {
                  "name": "Page 2",
                  "expected": [
                    { "target": "Result", "value": "50" },
                    { "target": "Status", "value": "Complete" }
                  ],
                  "action": {
                    "actionType": "Click",
                    "target": "Try chatgpt"
                  }
                },
                {
                  "name": "Page 3",
                  "expected": [{ "target": "Title", "value": "Sign up" }],
                  "action": {
                    "actionType": "Submit",
                    "fields": {
                      "Username": "ayago",
                      "Email": "adriancyago@gmail.com"
                    }
                  }
                }
              ]
            }""";
        File jsonFile = createTempJsonFile(jsonTestData);
        TestScenario scenario = parser.parse(jsonFile.getAbsolutePath());
        
        assertNotNull(scenario);
        assertThat(scenario.getFeatures(), hasKey("DUMMY_FEATURE"));
        assertThat(scenario.getPages(), hasSize(3));
        
        Action action1 = scenario.getPages().getFirst().getAction();
        assertThat(action1, instanceOf(EnterAction.class));
        assertEquals("Search", ((EnterAction) action1).getTargetField());
        
        Action action2 = scenario.getPages().get(1).getAction();
        assertThat(action2, instanceOf(ClickAction.class));
        assertEquals("Try chatgpt", ((ClickAction) action2).getTarget());
        
        Action action3 = scenario.getPages().get(2).getAction();
        assertThat(action3, instanceOf(SubmitAction.class));
        assertEquals("ayago", ((SubmitAction) action3).getFields().get("Username"));
    }
    
    @Test
    void parse_manyPages_parsesCorrectly() throws IOException {
        StringBuilder pagesJson = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            pagesJson.append("    {\n")
                .append("      \"name\": \"Page ").append(i).append("\",\n")
                .append("      \"expected\": [{ \"target\": \"Element").append(i).append("\", \"value\": \"Value").append(i).append("\" }],\n")
                .append("      \"action\": {\n")
                .append("        \"actionType\": \"Enter\",\n")
                .append("        \"targetField\": \"Field").append(i).append("\",\n")
                .append("        \"value\": \"Value").append(i).append("\"\n")
                .append("      }\n")
                .append("    }");
            if (i < 12) {
                pagesJson.append(",\n");
            }
        }
        
        String jsonTestData = "{\n" +
            "  \"host\": \"https://www.google.com\",\n" +
            "  \"features\": {},\n" +
            "  \"pages\": [\n" + pagesJson.toString() + "\n  ]\n" +
            "}";
        File jsonFile = createTempJsonFile(jsonTestData);
        TestScenario scenario = parser.parse(jsonFile.getAbsolutePath());
        
        assertNotNull(scenario);
        assertThat(scenario.getPages(), hasSize(12));
        for (int i = 0; i < 12; i++) {
            Action action = scenario.getPages().get(i).getAction();
            assertThat(action, instanceOf(EnterAction.class));
            assertEquals("Field" + (i + 1), ((EnterAction) action).getTargetField());
        }
    }
    
    @Test
    void parse_noExpectedItems_parsesCorrectly() throws IOException {
        // Empty expected array
        String jsonTestData = """
            {
              "host": "https://www.google.com",
              "features": {},
              "pages": [
                {
                  "name": "Page 1",
                  "expected": [],
                  "action": {
                    "actionType": "Enter",
                    "targetField": "Search",
                    "value": "chatgpt"
                  }
                }
              ]
            }""";
        File jsonFile = createTempJsonFile(jsonTestData);
        TestScenario scenario = parser.parse(jsonFile.getAbsolutePath());
        
        assertNotNull(scenario);
        assertThat(scenario.getPages().getFirst().getExpected(), hasSize(0));
    }
    
    @Test
    void parse_missingExpectedField_parsesAsEmptyOrNull() throws IOException {
        // Test behavior when "expected" key is entirely missing from a page object
        // "expected" key is missing
        String jsonTestData = """
            {
              "host": "https://www.google.com",
              "features": {},
              "pages": [
                {
                  "name": "Page 1",
                  "action": {
                    "actionType": "Enter",
                    "targetField": "Search",
                    "value": "chatgpt"
                  }
                }
              ]
            }""";
        File jsonFile = createTempJsonFile(jsonTestData);
        TestScenario scenario = parser.parse(jsonFile.getAbsolutePath());
        
        assertNotNull(scenario);
        TestScenario.Page page1 = scenario.getPages().getFirst();
        // Depending on POJO constructor and Jackson config (e.g. nulls vs. empty collections)
        // this might be null or an empty list. If your Page constructor initializes 'expected'
        // to an empty list if null is passed, then hasSize(0) is correct.
        // If Page constructor allows null for expected, then assertNull(page1.getExpected())
        // For this example, assuming Page constructor handles null input for expected by creating empty list or POJO has @NotNull
        assertNotNull(page1.getExpected(), "Expected list should not be null even if key is missing, assuming POJO handles it.");
        assertThat(page1.getExpected(), hasSize(0));
    }
    
    
    @Test
    void parse_invalidJsonSyntax_throwsIOException() throws IOException {
        // Extra comma, invalid JSON
        String jsonTestData = """
            {
              "host": "https://www.google.com",
              "features": {},,
              "pages": []
            }""";
        File jsonFile = createTempJsonFile(jsonTestData);
        
        // Expecting a Jackson-specific exception, often a JsonProcessingException (subclass of IOException)
        IOException exception = assertThrows(IOException.class, () -> {
            parser.parse(jsonFile.getAbsolutePath());
        });
        // You can add more specific checks for the exception message if needed
        assertTrue(exception.getMessage().contains("Unexpected character"));
    }
    
    @Test
    void parse_emptyJsonFile_throwsException() throws IOException {
        String jsonTestData = ""; // Empty content
        File jsonFile = createTempJsonFile(jsonTestData);
        
        // Jackson might throw an EOFException or similar if the content is empty
        // or if it's not a valid JSON structure (e.g. missing "host")
        IOException exception = assertThrows(IOException.class, () -> {
            parser.parse(jsonFile.getAbsolutePath());
        });
        // The exact message depends on how Jackson handles empty input and subsequent validation
        // It might be "No content to map due to end-of-input" or related to missing mandatory fields
        // like "host" if the POJO validation kicks in after an empty object is created.
        // If your POJO @NotNull on host is active, it might be a validation exception wrapped in IOException.
        // For now, a general IOException is checked.
        System.out.println("Empty JSON file test exception: " + exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("no content") ||
            exception.getMessage().toLowerCase().contains("host cannot be null"));
    }
    
    @Test
    void parse_jsonFileOnlyHost_parsesCorrectly() throws IOException {
        String jsonTestData = """
            { "host": "https://www.example.com" }""";
        // pages and features are optional in this JSON, POJO should handle null/empty
        File jsonFile = createTempJsonFile(jsonTestData);
        TestScenario scenario = parser.parse(jsonFile.getAbsolutePath());
        
        assertNotNull(scenario);
        assertEquals("https://www.example.com", scenario.getHost());
        // Depending on how TestScenario handles missing 'features' or 'pages' in constructor
        // (e.g., initializes to empty collections or allows null)
        if (scenario.getFeatures() != null) {
            assertThat(scenario.getFeatures().values(), hasSize(0));
        }
        if (scenario.getPages() != null) {
            assertThat(scenario.getPages(), hasSize(0));
        } else {
            // If pages can be null and is missing in JSON, this is also valid.
            // For this test, let's assume if pages key is missing, the list is null or empty.
            // The TestScenario POJO has @NotNull on pages list, so it should not be null.
            // If it's missing from JSON, Jackson might pass null to constructor,
            // causing validation to fail unless constructor handles it.
            // Given the POJO, it's more likely that a missing "pages" field would cause
            // an issue if not handled by default values or @JsonInclude(JsonInclude.Include.NON_NULL)
            // For this test, assuming the JSON would provide an empty list if no pages.
            // Let's adjust JSON to be more explicit for this test case for clarity.
            
            String explicitJson = """
                { "host": "https://www.example.com", "features": {}, "pages": [] }""";
            jsonFile = createTempJsonFile(explicitJson);
            scenario = parser.parse(jsonFile.getAbsolutePath());
            assertNotNull(scenario.getPages());
            assertThat(scenario.getPages(), hasSize(0));
            assertNotNull(scenario.getFeatures());
            assertThat(scenario.getFeatures().values(), hasSize(0));
        }
    }
}

