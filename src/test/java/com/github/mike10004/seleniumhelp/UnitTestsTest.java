package com.github.mike10004.seleniumhelp;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class UnitTestsTest {

    @Test
    public void getChromeOptionsExtraArgs() {
        String[][] testCases = {
                {null, "null"},
                {"", "empty string"},
                {" ", "single space char"},
                {"--no-sandbox", "one arg", "--no-sandbox"},
                {"--no-sandbox ", "trailing whitespace", "--no-sandbox"},
                {" --no-sandbox", "leading whitespace", "--no-sandbox"},
                {"--no-sandbox --disable-gpu", "two args", "--no-sandbox", "--disable-gpu"},
                {"--no-sandbox --proxy=1.2.3.4:5678", "two args with value", "--no-sandbox", "--proxy=1.2.3.4:5678"},
        };
        Stream.of(testCases).forEach(testCase -> {
            String input = testCase[0];
            String message = testCase[1];
            List<String> expected = Arrays.asList(testCase).subList(2, testCase.length);
            List<String> actual = UnitTests.getChromeOptionsExtraArgs(input);
            assertEquals(message, expected, actual);
        });

    }
}
