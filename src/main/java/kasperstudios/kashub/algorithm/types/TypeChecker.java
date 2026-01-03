package kasperstudios.kashub.algorithm.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypeChecker {
    private static final Logger LOGGER = LogManager.getLogger(TypeChecker.class);

    public enum Mode {
        OFF,
        LOOSE,
        STRICT
    }

    private static TypeChecker instance;
    private Mode mode = Mode.OFF;

    private final Map<String, KHType> variableTypes = new HashMap<>();

    private final List<TypeIssue> issues = new ArrayList<>();

    private static final Pattern TYPED_VAR_PATTERN =
        Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*:\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");

    private static final Pattern TYPE_DIRECTIVE_PATTERN =
        Pattern.compile("^\\s*@type\\s+(strict|loose|off)\\s*$", Pattern.CASE_INSENSITIVE);

    private TypeChecker() {}

    public static TypeChecker getInstance() {
        if (instance == null) {
            instance = new TypeChecker();
        }
        return instance;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        LOGGER.info("Type checking mode set to: {}", mode);
    }

    public Mode getMode() {
        return mode;
    }

    public void reset() {
        variableTypes.clear();
        issues.clear();
    }

    public boolean processDirective(String line) {
        Matcher matcher = TYPE_DIRECTIVE_PATTERN.matcher(line);
        if (matcher.find()) {
            String modeStr = matcher.group(1).toLowerCase();
            switch (modeStr) {
                case "strict":
                    setMode(Mode.STRICT);
                    break;
                case "loose":
                    setMode(Mode.LOOSE);
                    break;
                case "off":
                    setMode(Mode.OFF);
                    break;
            }
            return true;
        }
        return false;
    }

    public TypedVariableDeclaration parseTypedDeclaration(String line) {
        Matcher matcher = TYPED_VAR_PATTERN.matcher(line);
        if (matcher.find()) {
            String varName = matcher.group(1);
            String typeName = matcher.group(2);
            String value = matcher.group(3).trim();

            KHType type = KHType.fromString(typeName);
            return new TypedVariableDeclaration(varName, type, value);
        }
        return null;
    }

    public void registerVariable(String name, KHType type) {
        variableTypes.put(name, type);
    }

    public KHType getVariableType(String name) {
        return variableTypes.getOrDefault(name, KHType.ANY);
    }

    public boolean checkAssignment(String varName, String value, int lineNumber) {
        if (mode == Mode.OFF) {
            return true;
        }

        KHType declaredType = variableTypes.get(varName);
        if (declaredType == null || declaredType == KHType.ANY) {
            return true;
        }

        TypedValue typedValue = new TypedValue(value, declaredType);
        if (!typedValue.isValid()) {
            TypeIssue issue = new TypeIssue(
                lineNumber,
                varName,
                declaredType,
                typedValue.getType(),
                "Type mismatch: expected " + declaredType.getName() +
                ", got " + typedValue.getType().getName()
            );
            issues.add(issue);

            if (mode == Mode.STRICT) {
                LOGGER.error("Type error at line {}: {}", lineNumber, issue.getMessage());
                return false;
            } else {
                LOGGER.warn("Type warning at line {}: {}", lineNumber, issue.getMessage());
                return true;
            }
        }

        return true;
    }

    public boolean checkCommandArgs(String commandName, String[] args, KHType[] expectedTypes, int lineNumber) {
        if (mode == Mode.OFF || expectedTypes == null) {
            return true;
        }

        boolean valid = true;
        for (int i = 0; i < Math.min(args.length, expectedTypes.length); i++) {
            if (expectedTypes[i] == KHType.ANY) {
                continue;
            }

            TypedValue typedValue = new TypedValue(args[i]);
            if (!expectedTypes[i].isCompatibleWith(typedValue.getType())) {
                TypeIssue issue = new TypeIssue(
                    lineNumber,
                    commandName + " arg[" + i + "]",
                    expectedTypes[i],
                    typedValue.getType(),
                    "Argument type mismatch: expected " + expectedTypes[i].getName() +
                    ", got " + typedValue.getType().getName()
                );
                issues.add(issue);

                if (mode == Mode.STRICT) {
                    LOGGER.error("Type error at line {}: {}", lineNumber, issue.getMessage());
                    valid = false;
                } else {
                    LOGGER.warn("Type warning at line {}: {}", lineNumber, issue.getMessage());
                }
            }
        }

        return valid;
    }

    public List<TypeIssue> getIssues() {
        return new ArrayList<>(issues);
    }

    public boolean hasErrors() {
        return mode == Mode.STRICT && !issues.isEmpty();
    }

    public static class TypeIssue {
        private final int lineNumber;
        private final String variableName;
        private final KHType expectedType;
        private final KHType actualType;
        private final String message;

        public TypeIssue(int lineNumber, String variableName, KHType expectedType,
                        KHType actualType, String message) {
            this.lineNumber = lineNumber;
            this.variableName = variableName;
            this.expectedType = expectedType;
            this.actualType = actualType;
            this.message = message;
        }

        public int getLineNumber() { return lineNumber; }
        public String getVariableName() { return variableName; }
        public KHType getExpectedType() { return expectedType; }
        public KHType getActualType() { return actualType; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return String.format("Line %d: %s", lineNumber, message);
        }
    }

    public static class TypedVariableDeclaration {
        private final String name;
        private final KHType type;
        private final String value;

        public TypedVariableDeclaration(String name, KHType type, String value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        public String getName() { return name; }
        public KHType getType() { return type; }
        public String getValue() { return value; }
    }
}
