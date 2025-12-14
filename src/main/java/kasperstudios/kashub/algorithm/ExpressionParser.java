package kasperstudios.kashub.algorithm;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recursive descent expression parser for KHScript.
 * Supports proper operator precedence like JS/Rust:
 * - Parentheses (highest)
 * - Unary: !, - (negation)
 * - Multiplicative: *, /, %
 * - Additive: +, -
 * - Comparison: <, >, <=, >=
 * - Equality: ==, !=
 * - Logical AND: &&
 * - Logical OR: || (lowest)
 * - Ternary: ? :
 */
public class ExpressionParser {
    private final String input;
    private int pos;
    private final Function<String, String> variableResolver;
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    
    public ExpressionParser(String input, Function<String, String> variableResolver) {
        this.input = input.trim();
        this.pos = 0;
        this.variableResolver = variableResolver;
    }
    
    /**
     * Parse and evaluate an expression, returning the result as a Value
     */
    public Value parse() {
        skipWhitespace();
        if (pos >= input.length()) {
            return Value.ofString("");
        }
        Value result = parseTernary();
        skipWhitespace();
        return result;
    }
    
    /**
     * Parse ternary operator: condition ? trueExpr : falseExpr
     */
    private Value parseTernary() {
        Value condition = parseOr();
        skipWhitespace();
        
        if (pos < input.length() && input.charAt(pos) == '?') {
            pos++; // consume '?'
            skipWhitespace();
            Value trueValue = parseTernary();
            skipWhitespace();
            
            if (pos < input.length() && input.charAt(pos) == ':') {
                pos++; // consume ':'
                skipWhitespace();
                Value falseValue = parseTernary();
                return condition.toBoolean() ? trueValue : falseValue;
            }
        }
        return condition;
    }
    
    /**
     * Parse logical OR: expr || expr
     */
    private Value parseOr() {
        Value left = parseAnd();
        skipWhitespace();
        
        while (pos + 1 < input.length() && input.charAt(pos) == '|' && input.charAt(pos + 1) == '|') {
            pos += 2; // consume '||'
            skipWhitespace();
            Value right = parseAnd();
            left = Value.ofBoolean(left.toBoolean() || right.toBoolean());
            skipWhitespace();
        }
        return left;
    }
    
    /**
     * Parse logical AND: expr && expr
     */
    private Value parseAnd() {
        Value left = parseEquality();
        skipWhitespace();
        
        while (pos + 1 < input.length() && input.charAt(pos) == '&' && input.charAt(pos + 1) == '&') {
            pos += 2; // consume '&&'
            skipWhitespace();
            Value right = parseEquality();
            left = Value.ofBoolean(left.toBoolean() && right.toBoolean());
            skipWhitespace();
        }
        return left;
    }
    
    /**
     * Parse equality: expr == expr, expr != expr
     */
    private Value parseEquality() {
        Value left = parseComparison();
        skipWhitespace();
        
        while (pos + 1 < input.length()) {
            if (input.charAt(pos) == '=' && input.charAt(pos + 1) == '=') {
                pos += 2;
                skipWhitespace();
                Value right = parseComparison();
                left = Value.ofBoolean(left.equals(right));
            } else if (input.charAt(pos) == '!' && input.charAt(pos + 1) == '=') {
                pos += 2;
                skipWhitespace();
                Value right = parseComparison();
                left = Value.ofBoolean(!left.equals(right));
            } else {
                break;
            }
            skipWhitespace();
        }
        return left;
    }
    
    /**
     * Parse comparison: expr < expr, expr > expr, expr <= expr, expr >= expr
     */
    private Value parseComparison() {
        Value left = parseAdditive();
        skipWhitespace();
        
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '<' || c == '>') {
                boolean hasEquals = pos + 1 < input.length() && input.charAt(pos + 1) == '=';
                String op = hasEquals ? (c + "=") : String.valueOf(c);
                pos += hasEquals ? 2 : 1;
                skipWhitespace();
                Value right = parseAdditive();
                
                Double leftNum = left.toNumber();
                Double rightNum = right.toNumber();
                
                if (leftNum != null && rightNum != null) {
                    boolean result = switch (op) {
                        case "<" -> leftNum < rightNum;
                        case ">" -> leftNum > rightNum;
                        case "<=" -> leftNum <= rightNum;
                        case ">=" -> leftNum >= rightNum;
                        default -> false;
                    };
                    left = Value.ofBoolean(result);
                } else {
                    // String comparison
                    int cmp = left.toString().compareToIgnoreCase(right.toString());
                    boolean result = switch (op) {
                        case "<" -> cmp < 0;
                        case ">" -> cmp > 0;
                        case "<=" -> cmp <= 0;
                        case ">=" -> cmp >= 0;
                        default -> false;
                    };
                    left = Value.ofBoolean(result);
                }
            } else {
                break;
            }
            skipWhitespace();
        }
        return left;
    }
    
    /**
     * Parse additive: expr + expr, expr - expr
     */
    private Value parseAdditive() {
        Value left = parseMultiplicative();
        skipWhitespace();
        
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '+' || c == '-') {
                // Make sure it's not ++ or --
                if (pos + 1 < input.length() && input.charAt(pos + 1) == c) {
                    break;
                }
                pos++;
                skipWhitespace();
                Value right = parseMultiplicative();
                
                Double leftNum = left.toNumber();
                Double rightNum = right.toNumber();
                
                if (leftNum != null && rightNum != null) {
                    double result = c == '+' ? leftNum + rightNum : leftNum - rightNum;
                    left = Value.ofNumber(result);
                } else if (c == '+') {
                    // String concatenation
                    left = Value.ofString(left.toString() + right.toString());
                } else {
                    // Can't subtract strings
                    left = Value.ofNumber(Double.NaN);
                }
            } else {
                break;
            }
            skipWhitespace();
        }
        return left;
    }
    
    /**
     * Parse multiplicative: expr * expr, expr / expr, expr % expr
     */
    private Value parseMultiplicative() {
        Value left = parseUnary();
        skipWhitespace();
        
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '*' || c == '/' || c == '%') {
                pos++;
                skipWhitespace();
                Value right = parseUnary();
                
                Double leftNum = left.toNumber();
                Double rightNum = right.toNumber();
                
                if (leftNum != null && rightNum != null) {
                    double result = switch (c) {
                        case '*' -> leftNum * rightNum;
                        case '/' -> rightNum != 0 ? leftNum / rightNum : Double.NaN;
                        case '%' -> rightNum != 0 ? leftNum % rightNum : Double.NaN;
                        default -> Double.NaN;
                    };
                    left = Value.ofNumber(result);
                } else {
                    left = Value.ofNumber(Double.NaN);
                }
            } else {
                break;
            }
            skipWhitespace();
        }
        return left;
    }
    
    /**
     * Parse unary: !expr, -expr
     */
    private Value parseUnary() {
        skipWhitespace();
        
        if (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '!') {
                pos++;
                skipWhitespace();
                Value operand = parseUnary();
                return Value.ofBoolean(!operand.toBoolean());
            } else if (c == '-') {
                // Check if it's a negative number or unary minus
                if (pos + 1 < input.length() && !Character.isDigit(input.charAt(pos + 1))) {
                    pos++;
                    skipWhitespace();
                    Value operand = parseUnary();
                    Double num = operand.toNumber();
                    return num != null ? Value.ofNumber(-num) : Value.ofNumber(Double.NaN);
                }
            }
        }
        return parsePrimary();
    }
    
    /**
     * Parse primary: numbers, strings, booleans, variables, parentheses
     */
    private Value parsePrimary() {
        skipWhitespace();
        
        if (pos >= input.length()) {
            return Value.ofString("");
        }
        
        char c = input.charAt(pos);
        
        // Parentheses
        if (c == '(') {
            pos++; // consume '('
            Value result = parseTernary();
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == ')') {
                pos++; // consume ')'
            }
            return result;
        }
        
        // String literal
        if (c == '"' || c == '\'') {
            return parseString(c);
        }
        
        // Number (including negative)
        if (Character.isDigit(c) || (c == '-' && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1)))) {
            return parseNumber();
        }
        
        // Variable ($name or $NAME)
        if (c == '$') {
            return parseVariable();
        }
        
        // Boolean or identifier
        if (Character.isLetter(c) || c == '_') {
            return parseIdentifier();
        }
        
        // Unknown - return as string
        return Value.ofString(String.valueOf(c));
    }
    
    private Value parseString(char quote) {
        pos++; // consume opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && input.charAt(pos) != quote) {
            if (input.charAt(pos) == '\\' && pos + 1 < input.length()) {
                pos++;
                char escaped = input.charAt(pos);
                switch (escaped) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '\\' -> sb.append('\\');
                    case '"' -> sb.append('"');
                    case '\'' -> sb.append('\'');
                    default -> sb.append(escaped);
                }
            } else {
                sb.append(input.charAt(pos));
            }
            pos++;
        }
        if (pos < input.length()) {
            pos++; // consume closing quote
        }
        return Value.ofString(sb.toString());
    }
    
    private Value parseNumber() {
        int start = pos;
        if (input.charAt(pos) == '-') {
            pos++;
        }
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
            pos++;
        }
        String numStr = input.substring(start, pos).replace(',', '.');
        try {
            return Value.ofNumber(Double.parseDouble(numStr));
        } catch (NumberFormatException e) {
            return Value.ofString(numStr);
        }
    }
    
    private Value parseVariable() {
        pos++; // consume '$'
        int start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
            pos++;
        }
        String varName = input.substring(start, pos);
        String value = variableResolver.apply(varName);
        if (value == null) {
            return Value.ofString("");
        }
        // Try to parse as number
        try {
            return Value.ofNumber(Double.parseDouble(value.replace(',', '.')));
        } catch (NumberFormatException e) {
            return Value.ofString(value);
        }
    }
    
    private Value parseIdentifier() {
        int start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
            pos++;
        }
        String identifier = input.substring(start, pos);
        
        // Check for boolean literals
        if (identifier.equalsIgnoreCase("true")) {
            return Value.ofBoolean(true);
        }
        if (identifier.equalsIgnoreCase("false")) {
            return Value.ofBoolean(false);
        }
        if (identifier.equalsIgnoreCase("null")) {
            return Value.ofNull();
        }
        
        // Check if it's a variable without $
        String value = variableResolver.apply(identifier);
        if (value != null) {
            try {
                return Value.ofNumber(Double.parseDouble(value.replace(',', '.')));
            } catch (NumberFormatException e) {
                return Value.ofString(value);
            }
        }
        
        return Value.ofString(identifier);
    }
    
    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }
    
    /**
     * Value class representing a KHScript value (number, string, boolean, null)
     */
    public static class Value {
        public enum Type { NUMBER, STRING, BOOLEAN, NULL }
        
        private final Type type;
        private final Object value;
        
        private Value(Type type, Object value) {
            this.type = type;
            this.value = value;
        }
        
        public static Value ofNumber(double num) {
            return new Value(Type.NUMBER, num);
        }
        
        public static Value ofString(String str) {
            return new Value(Type.STRING, str);
        }
        
        public static Value ofBoolean(boolean bool) {
            return new Value(Type.BOOLEAN, bool);
        }
        
        public static Value ofNull() {
            return new Value(Type.NULL, null);
        }
        
        public Type getType() {
            return type;
        }
        
        public Double toNumber() {
            if (type == Type.NUMBER) {
                return (Double) value;
            }
            if (type == Type.STRING) {
                try {
                    return Double.parseDouble(((String) value).replace(',', '.'));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            if (type == Type.BOOLEAN) {
                return (Boolean) value ? 1.0 : 0.0;
            }
            return null;
        }
        
        public boolean toBoolean() {
            if (type == Type.BOOLEAN) {
                return (Boolean) value;
            }
            if (type == Type.NULL) {
                return false;
            }
            if (type == Type.NUMBER) {
                double num = (Double) value;
                return num != 0 && !Double.isNaN(num);
            }
            if (type == Type.STRING) {
                String str = (String) value;
                return !str.isEmpty() && !str.equals("0") && !str.equalsIgnoreCase("false") && !str.equalsIgnoreCase("null");
            }
            return false;
        }
        
        @Override
        public String toString() {
            if (type == Type.NULL) {
                return "null";
            }
            if (type == Type.NUMBER) {
                double num = (Double) value;
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            }
            if (type == Type.BOOLEAN) {
                return String.valueOf(value);
            }
            return String.valueOf(value);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Value other)) {
                return false;
            }
            
            // Null comparison
            if (type == Type.NULL || other.type == Type.NULL) {
                return type == other.type;
            }
            
            // Try numeric comparison first
            Double thisNum = this.toNumber();
            Double otherNum = other.toNumber();
            if (thisNum != null && otherNum != null) {
                return Math.abs(thisNum - otherNum) < 0.0001;
            }
            
            // String comparison (case-insensitive)
            return this.toString().equalsIgnoreCase(other.toString());
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }
    }
    
    /**
     * Convenience method to evaluate an expression string
     */
    public static Value evaluate(String expression, Function<String, String> variableResolver) {
        return new ExpressionParser(expression, variableResolver).parse();
    }
    
    /**
     * Convenience method to evaluate a condition and return boolean
     */
    public static boolean evaluateCondition(String expression, Function<String, String> variableResolver) {
        return evaluate(expression, variableResolver).toBoolean();
    }
}
