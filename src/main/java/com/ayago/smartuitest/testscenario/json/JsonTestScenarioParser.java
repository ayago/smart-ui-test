package com.ayago.smartuitest.testscenario.json;

import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.ClickAction;
import com.ayago.smartuitest.testscenario.EnterAction;
import com.ayago.smartuitest.testscenario.SubmitAction;
import com.ayago.smartuitest.testscenario.TestScenario;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.io.File;
import java.io.IOException;

/**
 * Parses a TestScenario from a JSON source using Jackson.
 * This parser is configured with MixIns to avoid annotating the POJOs directly
 * and uses a custom deserializer for the polymorphic Action interface.
 * The primary public parsing method is parseJsonFile(String jsonFileName).
 */
@Component
@Validated
public class JsonTestScenarioParser {
    
    private final ObjectMapper objectMapper;
    
    public JsonTestScenarioParser() {
        this.objectMapper = new ObjectMapper();
        
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        objectMapper.addMixIn(TestScenario.class, TestScenarioMixIns.class);
        objectMapper.addMixIn(TestScenario.Feature.class, TestScenarioMixIns.FeatureMixIn.class);
        objectMapper.addMixIn(TestScenario.Page.class, TestScenarioMixIns.PageMixIn.class);
        objectMapper.addMixIn(TestScenario.ExpectedElement.class, TestScenarioMixIns.ExpectedElementMixIn.class);
        
        objectMapper.addMixIn(ClickAction.class, ActionMixIns.ClickActionMixIn.class);
        objectMapper.addMixIn(EnterAction.class, ActionMixIns.EnterActionMixIn.class);
        objectMapper.addMixIn(SubmitAction.class, ActionMixIns.SubmitActionMixIn.class);
        
        SimpleModule actionModule = new SimpleModule("ActionDeserializerModule");
        actionModule.addDeserializer(Action.class, new ActionDeserializer());
        objectMapper.registerModule(actionModule);
    }
    
    /**
     * Parses a TestScenario from a JSON file, given its filename/path.
     * This is the sole public method for parsing TestScenario objects from a file.
     *
     * @param jsonFile The name or path of the JSON file.
     * @return The parsed TestScenario object.
     * @throws IOException              If there's an error reading the file or during JSON parsing/mapping.
     * @throws IllegalArgumentException if jsonFileName is null or empty.
     */
    public TestScenario parse(File jsonFile) throws IOException {
        return parseJsonFileInternal(jsonFile);
    }
    
    /**
     * Internal helper method to parse a TestScenario from a JSON file.
     *
     * @param jsonFile The File object pointing to the JSON file.
     * @return The parsed TestScenario object.
     * @throws IOException If there's an error reading the file or during JSON parsing/mapping.
     */
    private TestScenario parseJsonFileInternal(File jsonFile) throws IOException {
        if (jsonFile == null) {
            throw new IllegalArgumentException("JSON file object cannot be null.");
        }
        if (!jsonFile.exists()) {
            throw new IOException("JSON file does not exist: " + jsonFile.getAbsolutePath());
        }
        if (!jsonFile.isFile()) {
            throw new IOException("Path does not point to a regular file: " + jsonFile.getAbsolutePath());
        }
        if (!jsonFile.canRead()) {
            throw new IOException("Cannot read JSON file (check permissions): " + jsonFile.getAbsolutePath());
        }
        return objectMapper.readValue(jsonFile, TestScenario.class);
    }
}

