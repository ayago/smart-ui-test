package com.ayago.pageflow;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PageFlowParser {
    
    public PageFlowDefinition parse(String fileName) throws IOException {
        try (BufferedReader reader = prepareUITestFileReader(fileName)) {
            return parse(reader);
        }
    }
    
    public PageFlowDefinition parse(BufferedReader reader) throws IOException {
        String host = null;
        final List<PageModel> pages = new ArrayList<>();
        final List<FeatureFlag> featureFlags = new ArrayList<>();
        String line;
        PageModel currentPage = null;
        boolean inExpected = false;
        boolean inGiven = false;
        boolean parsingFeatures = false;
        boolean parsingPage = false;
        FeatureFlag currentFeature = null;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("Host:")) {
                host = line.substring(5).trim();
            } else if (line.startsWith("Features:")) {
                parsingFeatures = true;
                parsingPage = false;
            } else if (line.startsWith("Page ")) {
                parsingFeatures = false;
                parsingPage = true;
                if (currentPage != null) {
                    pages.add(currentPage);
                }
                final String name = line;
                final Map<String, String> expectedFields = new HashMap<>();
                final Map<String, String> givenFieldValues = new HashMap<>();
                
                currentPage = new PageModel(name, Collections.unmodifiableMap(expectedFields), Collections.unmodifiableMap(givenFieldValues), null);
                
            } else if (parsingFeatures && line.startsWith("- ")) {
                if (currentFeature != null) {
                    featureFlags.add(currentFeature);
                }
                currentFeature = new FeatureFlag(line.substring(2, line.indexOf(':')).trim(), false, Collections.unmodifiableMap(new HashMap<>())); //start with a new map
            } else if (parsingFeatures && line.startsWith("enable:")) {
                currentFeature = new FeatureFlag(currentFeature.getName(), Boolean.parseBoolean(line.substring(7).trim()), currentFeature.getContext());
            } else if (parsingFeatures && line.startsWith("on:")) {
                // skip line
            } else if (parsingFeatures && line.contains(":")) {
                String[] kv = line.split(":", 2);
                if (kv.length == 2) {
                    Map<String, String> modifiableContext = new HashMap<>(currentFeature.getContext()); //create a modifiable copy.
                    modifiableContext.put(kv[0].trim(), kv[1].trim());
                    currentFeature = new FeatureFlag(currentFeature.getName(), currentFeature.isEnabled(), Collections.unmodifiableMap(modifiableContext)); //create new FeatureFlag
                }
            } else if (parsingPage && line.startsWith("expected:")) {
                inExpected = true;
                inGiven = false;
            } else if (parsingPage && line.startsWith("given:")) {
                inGiven = true;
                inExpected = false;
            } else if (parsingPage && line.startsWith("action:")) {
                String actionButton = line.substring(7).trim();
                if(currentPage != null) { //make sure currentPage is not null
                    currentPage = new PageModel(currentPage.getName(), currentPage.getExpectedFields(), currentPage.getGivenFieldValues(), actionButton);
                }
                inGiven = inExpected = false;
            } else if (line.startsWith("- ")) {
                String[] parts = line.substring(2).split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    // Handle quoted values
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1).trim();
                    } else {
                        value = value.trim();
                    }
                    if (inExpected && currentPage != null) {
                        Map<String, String> modifiableMap = new HashMap<>(currentPage.getExpectedFields());
                        modifiableMap.put(key, value);
                        currentPage = new PageModel(currentPage.getName(), Collections.unmodifiableMap(modifiableMap), currentPage.getGivenFieldValues(), currentPage.getActionButton());
                    } else if (inGiven && currentPage != null) {
                        Map<String, String> modifiableMap = new HashMap<>(currentPage.getGivenFieldValues());
                        modifiableMap.put(key, value);
                        currentPage = new PageModel(currentPage.getName(), currentPage.getExpectedFields(), Collections.unmodifiableMap(modifiableMap), currentPage.getActionButton());
                    }
                }
            }
        }
        if (currentFeature != null) {
            featureFlags.add(currentFeature);
        }
        if (currentPage != null) {
            pages.add(currentPage);
        }
        return new PageFlowDefinition(host, Collections.unmodifiableList(featureFlags), Collections.unmodifiableList(pages));
    }
    
    private BufferedReader prepareUITestFileReader(String fileName) throws FileNotFoundException {
        File inputFile = new File(fileName);
        if (inputFile.exists()) {
            return new BufferedReader(new FileReader(inputFile));
        }
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(fileName);
        if (resourceStream == null) {
            throw new FileNotFoundException(String.format("%s not found in resources folder", fileName));
        }
        return new BufferedReader(new InputStreamReader(resourceStream));
    }
}