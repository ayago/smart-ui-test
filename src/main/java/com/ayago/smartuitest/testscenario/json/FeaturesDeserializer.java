package com.ayago.smartuitest.testscenario.json;

import com.ayago.smartuitest.testscenario.TestScenario.Feature;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Custom Jackson deserializer for a {@code Map<String, TestScenario.Feature>}.
 * This deserializer ensures that the key of each entry in the JSON features map
 * is used as the 'name' property of the corresponding {@link Feature} object.
 */
public class FeaturesDeserializer extends JsonDeserializer<Map<String, Feature>> {
    
    /**
     * Deserializes a JSON object into a {@code Map<String, Feature>}.
     * The keys of the JSON object become the names of the Feature objects.
     *
     * @param jp   The JsonParser used for reading JSON content.
     * @param ctxt The DeserializationContext.
     * @return A map where keys are feature names and values are {@link Feature} objects.
     * @throws IOException If an input/output error occurs.
     * @throws JsonProcessingException If the JSON content is malformed.
     */
    @Override
    public Map<String, Feature> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        // Read the entire JSON object that represents the "features" map
        ObjectNode featuresNode = mapper.readTree(jp);
        
        Map<String, Feature> featuresMap = new HashMap<>();
        
        // Iterate over each field (entry) in the JSON object for features
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = featuresNode.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = fieldsIterator.next();
            String featureName = entry.getKey(); // This is the "DUMMY_FEATURE" string
            JsonNode featureValueNode = entry.getValue(); // This is the {"enable": false, "context": {...}} object
            
            if (!featureValueNode.isObject()) {
                throw ctxt.weirdStringException(null, Map.class,
                    "Expected a JSON object for feature '" + featureName + "', but found " + featureValueNode.getNodeType());
            }
            
            // Extract 'enable' and 'context' from the featureValueNode
            JsonNode enableNode = featureValueNode.get("enable");
            JsonNode contextNode = featureValueNode.get("context");
            
            if (enableNode == null || !enableNode.isBoolean()) {
                throw ctxt.weirdStringException(null, Feature.class,
                    "Missing or invalid 'enable' field (boolean expected) for feature '" + featureName + "'.");
            }
            boolean enable = enableNode.asBoolean();
            
            Map<String, String> contextMap = new HashMap<>();
            if (contextNode != null) {
                if (!contextNode.isObject()) {
                    throw ctxt.weirdStringException(null, Feature.class,
                        "Expected a JSON object for 'context' in feature '" + featureName + "', but found " + contextNode.getNodeType());
                }
                // Deserialize the context node into a Map<String, String>
                // TypeReference could be used for more complex maps, but for Map<String, String>, treeToValue works.
                contextMap = mapper.treeToValue(contextNode, Map.class); // Jackson can infer Map<String,String> here
            }
            
            // Construct the Feature object using the name from the key, and enable/context from the value
            Feature feature = new Feature(enable, contextMap, featureName);
            featuresMap.put(featureName, feature);
        }
        return featuresMap;
    }
}

