package com.ayago.smartuitest.testscenario.json;

import com.ayago.smartuitest.testscenario.Action;
import com.ayago.smartuitest.testscenario.ClickAction;
import com.ayago.smartuitest.testscenario.EnterAction;
import com.ayago.smartuitest.testscenario.SubmitAction;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Custom Jackson deserializer for the {@link Action} interface.
 * It determines the concrete Action type based on an "actionType" field
 * in the JSON input and then deserializes to the specific class.
 */
public class ActionDeserializer extends JsonDeserializer<Action> {
    
    /**
     * Deserializes JSON content into an instance of a concrete class implementing {@link Action}.
     * It expects an "actionType" field in the JSON object to determine which
     * concrete class (ClickAction, EnterAction, SubmitAction) to use.
     *
     * @param jp      The JsonParser used for reading JSON content.
     * @param ctxt    The DeserializationContext that can be used to access information about
     * this deserialization activity.
     * @return An instance of a concrete Action implementation.
     * @throws IOException If an input or output exception occurs.
     * @throws JsonProcessingException If the JSON content does not conform to the expected structure.
     */
    @Override
    public Action deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        
        // Get the ObjectMapper from the JsonParser to use its configuration (including MixIns)
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        // Read the current JSON structure as a tree of JsonNode objects
        JsonNode node = mapper.readTree(jp);
        
        // Check for the presence of the "actionType" field, which acts as our discriminator.
        JsonNode actionTypeNode = node.get("actionType");
        if (actionTypeNode == null || !actionTypeNode.isTextual()) {
            throw ctxt.weirdStringException(null, Action.class,
                "Missing or invalid 'actionType' field in JSON for Action deserialization. Expected a string.");
        }
        String actionType = actionTypeNode.asText();
        
        // Based on the value of "actionType", deserialize the node into the appropriate concrete class.
        // The mapper.treeToValue() method will respect any registered MixIns for these concrete classes.
        switch (actionType) {
            case "Click":
                return mapper.treeToValue(node, ClickAction.class);
            case "Enter":
                return mapper.treeToValue(node, EnterAction.class);
            case "Submit":
                return mapper.treeToValue(node, SubmitAction.class);
            default:
                // If the actionType is unknown, throw an exception.
                throw ctxt.weirdStringException(actionType, Action.class,
                    "Unknown actionType '" + actionType + "'. Supported types are Click, Enter, Submit.");
        }
    }
}

