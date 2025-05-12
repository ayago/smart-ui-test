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
public class TestScenarioParser{
    private final ActionFactory actionFactory;
    
    public TestScenarioParser(ActionFactory actionFactory){
        this.actionFactory = actionFactory;
    }
    
    public TestScenario parse(BufferedReader reader) throws IOException{
        String host = null;
        Map<String, Feature> features = new HashMap<>();
        List<Page> pages = new ArrayList<>();
        String line;
        int lineNumber = 0;
        boolean pageWasLastLine = false;
        
        try{
            while ((line = reader.readLine()) != null){
                lineNumber++;
                line = line.trim();
                if (line.startsWith("Host:")) {
                    host = parseHost(line);
                } else if (line.equals("Features:")) {
                    var result = parseFeatures(reader);
                    features = result.object();
                    pageWasLastLine = lastScannedLineStartsWithPage(result);
                } else if (pageWasLastLine || line.startsWith("Page")) {
                    var result = parsePage(line, reader);
                    pages.add(result.object());
                    pageWasLastLine = lastScannedLineStartsWithPage(result);
                } else {
                    if (!line.isEmpty() && !line.startsWith("//")) {
                        throw new IOException("Invalid line format on line " + lineNumber + ": " + line);
                    }
                    
                    //  The logic to handle "-" lines outside "Features:" was here.  It is no longer needed, the logic is fine.
                }
            }
        } catch (IOException e) {
            throw new IOException("Error parsing test scenario: " + e.getMessage(), e);
        }
        
        return new TestScenario(host, features, pages);
    }
    
    private boolean lastScannedLineStartsWithPage(Tuple<?> result){
        String lastLine = result.lastLine();
        if(!StringUtils.hasLength(lastLine)){
            return false;
        }
        return lastLine.trim().startsWith("Page");
    }
    
    private String parseHost(String line) throws IOException{
        return line.substring(line.indexOf(":") + 1).trim();
    }
    
    private Tuple<Map<String, Feature>> parseFeatures(BufferedReader reader) throws IOException {
        Map<String, Feature> features = new HashMap<>();
        String line;
        boolean continueReading = true;
        String lastLineFromSubParsing = null;
        while (continueReading && (line = reader.readLine()) != null && line.trim().startsWith("-")) {
            var response = parseFeature(line, reader);
            var feature = response.object();
            features.put(feature.getName(), feature);
            lastLineFromSubParsing = response.lastLine().trim();
            continueReading = lastLineFromSubParsing.startsWith("-");
        }
        return new Tuple<>(lastLineFromSubParsing, features);
    }
    
    private Tuple<Feature> parseFeature(String line, BufferedReader reader) throws IOException{
        
        String featureName = line.substring(line.indexOf("-") + 1, line.indexOf(":")).trim();
        
        boolean enable = false;
        Map<String, String> context = new HashMap<>();
        String subLine = null;
        
        boolean continueReading = true;
        while (continueReading && (subLine = reader.readLine()) != null && !subLine.trim().startsWith("Page") && !subLine.trim().startsWith("-")){
            subLine = subLine.trim();
            if (subLine.startsWith("enable:")) {
                enable = Boolean.parseBoolean(subLine.substring(subLine.indexOf(":") + 1).trim());
            } else if (subLine.contains(":")) {
                String[] parts = subLine.split(":");
                if (parts.length == 2) {
                    context.put(parts[0].trim(), parts[1].trim());
                }
            }
            
            continueReading = !(subLine.trim().startsWith("Page") || subLine.trim().startsWith("-"));
        }
        return new Tuple<>(subLine, new Feature(enable, context, featureName));
    }
    
    private Tuple<Page> parsePage(String line, BufferedReader reader) throws IOException{
        String pageName = line.substring(line.indexOf("Page") + 4).trim();
        List<ExpectedElement> expectedList = parseExpected(reader);
        var parseActionResult = parseAction(reader);
        Action action = parseActionResult.object();
        Page page = new Page(pageName, expectedList, action);
        return new Tuple<>(parseActionResult.lastLine(), page);
    }
    
    private List<ExpectedElement> parseExpected(BufferedReader reader) throws IOException{
        List<ExpectedElement> expectedList = new ArrayList<>();
        String subLine;
        while ((subLine = reader.readLine()) != null && !subLine.trim().startsWith("action:")){
            subLine = subLine.trim();
            if (subLine.startsWith("-")) {
                String[] parts = subLine.substring(1).split(":");
                String target = parts[0].trim();
                String value = parts[1].trim().replace("\"", "");
                expectedList.add(new ExpectedElement(target, value));
            }
        }
        return expectedList;
    }
    
    private Tuple<Action> parseAction(BufferedReader reader) throws IOException{
        String type = null;
        Map<String, String> data = new HashMap<>();
        String subLine;
        if ((subLine = reader.readLine()) != null && !subLine.trim().startsWith("Page")) {
            subLine = subLine.trim();
            if (subLine.startsWith("type:")) {
                type = subLine.substring(subLine.indexOf(":") + 1).trim();
                data.put("type", type);
            }
        }
        
        if (type == null) {
            return new Tuple<>(subLine, null);
        }
        
        while ((subLine = reader.readLine()) != null && !subLine.trim().isEmpty()){
            subLine = subLine.trim();
            if (subLine.startsWith("target-field:")) {
                String targetField = subLine.substring(subLine.indexOf(":") + 1).trim();
                data.put("targetField", targetField);
            } else if (subLine.startsWith("target:")) {
                String target = subLine.substring(subLine.indexOf(":") + 1).trim();
                data.put("target", target);
            } else if (subLine.startsWith("value:")) {
                String value = subLine.substring(subLine.indexOf(":") + 1).trim();
                data.put("value", value);
            } else if (subLine.startsWith("fields:")) {
                Map<String, String> newFields = parseFields(reader);
                data.putAll(newFields);
            }
        }
        
        try{
            ActionType actionType = ActionType.valueOf(type);
            Action action = actionFactory.getAction(actionType, data);
            return new Tuple<>(subLine, action);
        } catch (IllegalArgumentException e) {
            return new Tuple<>(subLine, null);
        }
    }
    
    private Map<String, String> parseFields(BufferedReader reader) throws IOException{
        Map<String, String> fields = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null && line.trim().startsWith("-")){
            line = line.trim();
            String[] parts = line.substring(1).split(":");
            if (parts.length == 2) {
                String name = parts[0].trim();
                String val = parts[1].trim();
                fields.put("fields." + name, val);
            }
        }
        return fields;
    }
    
    private record Tuple<T>(String lastLine, T object) {}
}

