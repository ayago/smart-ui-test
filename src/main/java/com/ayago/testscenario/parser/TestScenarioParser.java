package com.ayago.testscenario.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.ayago.testscenario.Action;
import com.ayago.testscenario.TestScenario;
import com.ayago.testscenario.TestScenario.ExpectedElement;
import com.ayago.testscenario.TestScenario.Feature;
import com.ayago.testscenario.TestScenario.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
public class TestScenarioParser {
    
    // Use descriptive names for constants
    private static final String HOST_PREFIX = "Host:";
    private static final String FEATURES_KEYWORD = "Features:";
    private static final String PAGE_PREFIX = "Page";
    private static final String ACTION_PREFIX = "action:";
    private static final String ENABLE_PREFIX = "enable:";
    private static final String FIELD_SEPARATOR = ":";
    private static final String LIST_ITEM_PREFIX = "-";
    private static final String QUOTE = "\"";
    private static final String COMMENT_PREFIX = "//";
    private static final String FIELDS_PREFIX = "fields:"; // Added for consistency
    
    // Use a record for internal data transfer
    private record Tuple<T>(String lastLine, T object) {
    }
    
    private final ActionFactory actionFactory;
    
    public TestScenarioParser(ActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    }
    
    public TestScenario parse(BufferedReader reader) throws IOException {
        String host = null;
        Map<String, Feature> features = new HashMap<>();
        List<Page> pages = new ArrayList<>();
        String line;
        int lineNumber = 0;
        boolean pageWasLastLine = false;
        
        try {
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                if (line.startsWith(HOST_PREFIX)) {
                    host = parseHost(line, lineNumber);
                } else if (line.equals(FEATURES_KEYWORD)) {
                    Tuple<Map<String, Feature>> featuresResult = parseFeatures(reader, lineNumber);
                    features = featuresResult.object();
                    pageWasLastLine = lastScannedLineStartsWithPage(featuresResult.lastLine());
                } else if (pageWasLastLine || line.startsWith(PAGE_PREFIX)) {
                    Tuple<Page> pageResult = parsePage(line, reader, lineNumber);
                    pages.add(pageResult.object());
                    pageWasLastLine = lastScannedLineStartsWithPage(pageResult.lastLine());
                } else if (!line.isEmpty() && !line.startsWith(COMMENT_PREFIX)) {
                    throw new IOException("Invalid line format on line " + lineNumber + ": " + line);
                }
                // handle empty lines and comments
            }
        } catch (IOException e) {
            throw new IOException("Error parsing test scenario: " + e.getMessage(), e);
        }
        
        // Post-validation:  Check for mandatory 'Host'
        if (host == null) {
            throw new IOException("Missing 'Host' definition in test scenario.");
        }
        return new TestScenario(host, features, pages);
    }
    
    private boolean lastScannedLineStartsWithPage(String lastLine) {
        return StringUtils.hasLength(lastLine) && lastLine.trim().startsWith(PAGE_PREFIX);
    }
    
    private String parseHost(String line, int lineNumber) throws IOException {
        if (!line.contains(FIELD_SEPARATOR)) {
            throw new IOException("Invalid Host format at line " + lineNumber + ": " + line);
        }
        return line.substring(HOST_PREFIX.length()).trim();
    }
    
    private Tuple<Map<String, Feature>> parseFeatures(BufferedReader reader, int lineNumber) throws IOException {
        Map<String, Feature> features = new HashMap<>();
        String line;
        String lastLineFromSubParsing = null;
        while ((line = reader.readLine()) != null && line.trim().startsWith(LIST_ITEM_PREFIX)) {
            Tuple<Feature> featureResult = parseFeature(line, reader, lineNumber);
            Feature feature = featureResult.object();
            features.put(feature.getName(), feature);
            lastLineFromSubParsing = featureResult.lastLine();
        }
        return new Tuple<>(lastLineFromSubParsing, features);
    }
    
    private Tuple<Feature> parseFeature(String line, BufferedReader reader, int lineNumber) throws IOException {
        String featureName;
        try {
            featureName = line.substring(LIST_ITEM_PREFIX.length(), line.indexOf(FIELD_SEPARATOR)).trim();
        } catch (StringIndexOutOfBoundsException e) {
            throw new IOException("Invalid feature format at line " + lineNumber + ": " + line, e);
        }
        
        boolean enable = false;
        Map<String, String> on = new HashMap<>();
        String subLine;
        
        while ((subLine = reader.readLine()) != null && !subLine.trim().startsWith(PAGE_PREFIX) && !subLine.trim().startsWith(LIST_ITEM_PREFIX)) {
            subLine = subLine.trim();
            if (subLine.startsWith(ENABLE_PREFIX)) {
                enable = Boolean.parseBoolean(subLine.substring(ENABLE_PREFIX.length()).trim());
            } else if (subLine.contains(FIELD_SEPARATOR)) {
                String[] parts = subLine.split(FIELD_SEPARATOR);
                if (parts.length == 2) {
                    on.put(parts[0].trim(), parts[1].trim());
                }
            }
            // handle other cases
        }
        return new Tuple<>(subLine, new Feature(enable, on, featureName));
    }
    
    private Tuple<Page> parsePage(String line, BufferedReader reader, int lineNumber) throws IOException {
        String pageName = line.substring(PAGE_PREFIX.length()).trim();
        List<ExpectedElement> expectedList = parseExpected(reader, lineNumber);
        Tuple<Action> actionResult = parseAction(reader, lineNumber);
        Action action = actionResult.object();
        Page page = new Page(pageName, expectedList, action);
        return new Tuple<>(actionResult.lastLine(), page);
    }
    
    private List<ExpectedElement> parseExpected(BufferedReader reader, int lineNumber) throws IOException {
        List<ExpectedElement> expectedList = new ArrayList<>();
        String subLine;
        while ((subLine = reader.readLine()) != null && !subLine.trim().startsWith(ACTION_PREFIX)) {
            subLine = subLine.trim();
            if (subLine.startsWith(LIST_ITEM_PREFIX)) {
                String[] parts = subLine.substring(LIST_ITEM_PREFIX.length()).split(FIELD_SEPARATOR);
                if (parts.length == 2) {
                    String target = parts[0].trim();
                    String value = parts[1].trim().replace(QUOTE, "");
                    expectedList.add(new ExpectedElement(target, value));
                } else {
                    throw new IOException("Invalid expected element format at line " + lineNumber + ": " + subLine);
                }
            }
        }
        return expectedList;
    }
    
    private Tuple<Action> parseAction(BufferedReader reader, int lineNumber) throws IOException {
        String type = null;
        Map<String, String> data = new HashMap<>();
        String subLine;
        
        // Read until the end of the action block, or the next Page definition
        while ((subLine = reader.readLine()) != null && !subLine.trim().startsWith(PAGE_PREFIX)) {
            subLine = subLine.trim();
            String trimmedValue = subLine.substring(subLine.indexOf(":") + 1).trim();
            if (subLine.startsWith("type:")) {
                type = trimmedValue;
                data.put("type", type);
            } else if (subLine.startsWith("target-field:")) {
                data.put("targetField", trimmedValue);
            } else if (subLine.startsWith("target:")) {
                data.put("target", trimmedValue);
            } else if (subLine.startsWith("value:")) {
                data.put("value", trimmedValue);
            } else if (subLine.startsWith(FIELDS_PREFIX)) {
                Map<String, String> newFields = parseFields(reader, lineNumber);
                data.putAll(newFields);
            }
        }
        
        if (type == null) {
            throw new IOException("Action type is missing at line " + lineNumber);
        }
        
        try {
            ActionType actionType = ActionType.valueOf(type);
            Action action =  actionFactory.getAction(actionType, data);
            return new Tuple<>(subLine, action);
        } catch (IllegalArgumentException e) {
            throw new IOException("Unknown action type at line " + lineNumber + ": " + type, e);
        }
    }
    
    private Map<String, String> parseFields(BufferedReader reader, int lineNumber) throws IOException {
        Map<String, String> fields = new HashMap<>();
        String subLine;
        while ((subLine = reader.readLine()) != null && subLine.trim().startsWith(LIST_ITEM_PREFIX)) {
            subLine = subLine.trim();
            String[] parts = subLine.substring(LIST_ITEM_PREFIX.length()).split(FIELD_SEPARATOR);
            if (parts.length == 2) {
                String name = parts[0].trim();
                String value = parts[1].trim();
                fields.put(name, value);
            } else {
                throw new IOException("Invalid field format at line " + lineNumber + ": " + subLine);
            }
        }
        return fields;
    }
}
