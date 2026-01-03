package kasperstudios.kashub.services.runtime;

import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;

public class RegexTest {
    @Test
    public void testPatterns() {
        System.out.println("Testing patterns...");
        test("VARIABLE_PATTERN", "^\\s*(?:let\\s+|const\\s+)?([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
        test("IF_PATTERN", "^\\s*if\\s+(.+?)\\s*\\{\\s*$|^\\s*if\\s*\\((.*)\\)\\s*\\{?\\s*$");
        // Check potentially problematic nested braces or escapes?
        test("ELSE_IF_PATTERN",
                "^\\s*\\}?\\s*else\\s+if\\s+(.+?)\\s*\\{\\s*$|^\\s*\\}?\\s*else\\s+if\\s*\\((.*)\\)\\s*\\{?\\s*$");
        test("ELSE_PATTERN", "^\\s*\\}?\\s*else\\s*\\{?\\s*$");
        test("FOR_PATTERN", "^\\s*for\\s*\\((.*)\\)\\s*\\{?\\s*$");
        test("WHILE_PATTERN", "^\\s*while\\s+(.+?)\\s*\\{\\s*$|^\\s*while\\s*\\((.*)\\)\\s*\\{?\\s*$");
        test("LOOP_PATTERN", "^\\s*loop(?:\\s+(\\d+))?\\s*\\{?\\s*$");
        test("FUNCTION_PATTERN", "^\\s*function\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*?)\\)\\s*\\{?\\s*$");
        test("FUNCTION_CALL_PATTERN", "^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*?)\\)\\s*$");
        test("ENV_VAR_PATTERN", "\\$([A-Z_][A-Z0-9_]*)");
        test("USER_VAR_PATTERN", "\\$([a-z_][a-z0-9_]*)");
        System.out.println("All patterns valid.");
    }

    private void test(String name, String regex) {
        try {
            Pattern.compile(regex);
            System.out.println(name + " OK");
        } catch (Exception e) {
            System.err.println("FAILED: " + name + " - " + e.getMessage());
            throw e;
        }
    }
}
