package com.ayago.testscenario.parser;

import com.ayago.testscenario.Action;
import com.ayago.testscenario.ClickAction;
import com.ayago.testscenario.EnterAction;
import com.ayago.testscenario.SubmitAction;
import com.ayago.testscenario.TestScenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
public class TestScenarioParserTest {
    
    @Mock
    private ActionFactory actionFactory;
    
    @InjectMocks
    private TestScenarioParser parser;
    
    private BufferedReader createReader(String testData) {
        return new BufferedReader(new StringReader(testData));
    }
    
    @Test
    void parse_validScenario_returnsTestScenario() throws IOException {
        String testData = "Host: https://www.google.com\n" +
            "\n" +
            "Page 1\n" +
            "expected:\n" +
            " - Search: \"\"\n" +
            "action:\n" +
            " type: Enter\n" +
            " target-field: Search\n" +
            " value: chatgpt\n";
        
        when(actionFactory.getAction(eq(ActionType.Enter), anyMap()))
            .thenReturn(new EnterAction("Search", "chatgpt"));
        
        TestScenario scenario = parser.parse(createReader(testData));
        
        assertNotNull(scenario);
        assertEquals("https://www.google.com", scenario.getHost());
        assertThat(scenario.getPages(), hasSize(1));
    }
    
    @Test
    void parse_noFeatures_returnsTestScenario() throws IOException {
        String testData = "Host: https://www.google.com\n" +
            "\n" +
            "Page 1\n" +
            "expected:\n" +
            " - Search: \"\"\n" +
            "action:\n" +
            " type: Enter\n" +
            " target-field: Search\n" +
            " value: chatgpt\n";
        
        when(actionFactory.getAction(eq(ActionType.Enter), anyMap()))
            .thenReturn(new EnterAction("Search", "chatgpt"));
        
        TestScenario scenario = parser.parse(createReader(testData));
        
        assertNotNull(scenario);
        assertEquals("https://www.google.com", scenario.getHost());
        assertThat(scenario.getFeatures().values(), hasSize(0));
    }
    
    @Test
    void parse_threeActionTypes_parsesCorrectly() throws IOException {
        String testData = "Host: https://www.google.com\n" +
            "\n" +
            "Features:\n" +
            " - DUMMY_FEATURE:\n" +
            "  enable: false\n" +
            "  on:\n" +
            "   province: N/A\n" +
            "   store: N/A\n" +
            "\n" +
            "Page 1\n" +
            "expected:\n" +
            " - Search: \"\"\n" +
            "action:\n" +
            " type: Enter\n" +
            " target-field: Search\n" +
            " value: chatgpt\n" +
            "\n" +
            "Page 2\n" +
            "expected:\n" +
            " - Result: 50\n" +
            " - Status: Complete\n" +
            "action:\n" +
            " type: Click\n" +
            " target: Try chatgpt\n" +
            "\n" +
            "Page 3\n" +
            "expected:\n" +
            " - Title: Sign up\n" +
            "action:\n" +
            " type: Submit\n" +
            " fields: \n" +
            "  - Username: ayago\n" +
            "  - Email: adriancyago@gmail.com";
        
        when(actionFactory.getAction(eq(ActionType.Enter), anyMap()))
            .thenReturn(new EnterAction("Search", "chatgpt"));
        when(actionFactory.getAction(eq(ActionType.Click), anyMap()))
            .thenReturn(new ClickAction("Try chatgpt"));
        when(actionFactory.getAction(eq(ActionType.Submit), anyMap()))
            .thenReturn(new SubmitAction(Map.of("Username", "ayago", "Email", "adriancyago@gmail.com")));
        
        TestScenario scenario = parser.parse(createReader(testData));
        
        assertNotNull(scenario);
        assertThat(scenario.getPages(), hasSize(3));
        Action action1 = scenario.getPages().get(0).getAction();
        Action action2 = scenario.getPages().get(1).getAction();
        Action action3 = scenario.getPages().get(2).getAction();
        
        assertThat(action1, instanceOf(EnterAction.class));
        assertThat(action2, instanceOf(ClickAction.class));
        assertThat(action3, instanceOf(SubmitAction.class));
    }
    
    @Test
    void parse_manyPages_parsesCorrectly() throws IOException {
        StringBuilder testData = new StringBuilder("Host: https://www.google.com\n");
        
        Action[] stubActions = new Action[12];
        for (int i = 1; i <= 12; i++) {
            testData.append("\nPage ").append(i).append("\n");
            testData.append("expected:\n");
            testData.append(" - Element").append(i).append(": \"Value").append(i).append("\"\n");
            testData.append("action:\n");
            testData.append(" type: Enter\n");
            testData.append(" target-field: Field").append(i).append("\n");
            testData.append(" value: Value").append(i).append("\n");
            stubActions[i - 1] = new EnterAction("Field" + i, "Value" + i);
        }
        
        when(actionFactory.getAction(eq(ActionType.Enter), anyMap()))
            .thenReturn(stubActions[0], Arrays.copyOfRange(stubActions, 2, 11));
        
        TestScenario scenario = parser.parse(createReader(testData.toString()));
        
        assertNotNull(scenario);
        assertThat(scenario.getPages(), hasSize(12));
    }
    
    @Test
    void parse_noExpectedFields_parsesCorrectly() throws IOException {
        String testData = "Host: https://www.google.com\n" +
            "\n" +
            "Page 1\n" +
            "action:\n" +
            " type: Enter\n" +
            " target-field: Search\n" +
            " value: chatgpt\n";
        
        when(actionFactory.getAction(eq(ActionType.Enter), anyMap()))
            .thenReturn(new EnterAction("Search", "chatgpt"));
        
        TestScenario scenario = parser.parse(createReader(testData));
        
        assertNotNull(scenario);
        assertThat(scenario.getPages().get(0).getExpected(), hasSize(0));
    }
    
    @Test
    void parse_invalidInput_throwsIOException() {
        String testData = "Invalid Format";
        
        assertThrows(IOException.class, () -> parser.parse(createReader(testData)));
    }
    
}
