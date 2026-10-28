package kasperstudios.kashub.core;

import java.util.List;
import java.util.Objects;

/**
 * Parser - Enhanced expression parser for the engine.
 * Directly uses Value and Context for efficient evaluation.
 * Supports arithmetic, logic, comparison, and ternary operators.
 */
public class Parser {
    private static final int MAX_RECURSION_DEPTH = 256;
    private final String input;
    private int pos;
    private final Context ctx;
    private int depth = 0;

    public Parser(String input, Context ctx) {
        this.input = input != null ? input.trim() : "";
        this.ctx = ctx;
        this.pos = 0;
    }

    public Value parse() {
        if (input.isEmpty()) return Value.NULL;
        try {
            skipWhitespace();
            if (pos >= input.length())
                return Value.NULL;
            Value result = parseTernary();
            skipWhitespace();
            return result;
        } catch (StackOverflowError e) {
            throw new RuntimeException("Expression too complex (StackOverflow)");
        }
    }

    private void checkDepth() {
        if (++depth > MAX_RECURSION_DEPTH) {
            throw new RuntimeException("Maximum recursion depth exceeded (" + MAX_RECURSION_DEPTH + ")");
        }
    }

    private void exitDepth() {
        depth--;
    }

    private Value parseTernary() {
        checkDepth();
        try {
            Value condition = parseOr();
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == '?') {
                pos++;
                skipWhitespace();
                Value trueValue = parseTernary();
                skipWhitespace();
                if (pos < input.length() && input.charAt(pos) == ':') {
                    pos++;
                    skipWhitespace();
                    Value falseValue = parseTernary();
                    return condition.asBoolean() ? trueValue : falseValue;
                }
            }
            return condition;
        } finally {
            exitDepth();
        }
    }

    private Value parseOr() {
        Value left = parseAnd();
        skipWhitespace();
        while (pos + 1 < input.length() && input.charAt(pos) == '|' && input.charAt(pos + 1) == '|') {
            pos += 2;
            skipWhitespace();
            Value right = parseAnd();
            left = Value.of(left.asBoolean() || right.asBoolean());
            skipWhitespace();
        }
        return left;
    }

    private Value parseAnd() {
        Value left = parseEquality();
        skipWhitespace();
        while (pos + 1 < input.length() && input.charAt(pos) == '&' && input.charAt(pos + 1) == '&') {
            pos += 2;
            skipWhitespace();
            Value right = parseEquality();
            left = Value.of(left.asBoolean() && right.asBoolean());
            skipWhitespace();
        }
        return left;
    }

    private Value parseEquality() {
        Value left = parseComparison();
        skipWhitespace();
        while (pos + 1 < input.length()) {
            if (input.charAt(pos) == '=' && input.charAt(pos + 1) == '=') {
                pos += 2;
                skipWhitespace();
                Value right = parseComparison();
                left = Value.of(left.equals(right));
            } else if (input.charAt(pos) == '!' && input.charAt(pos + 1) == '=') {
                pos += 2;
                skipWhitespace();
                Value right = parseComparison();
                left = Value.of(!left.equals(right));
            } else {
                break;
            }
            skipWhitespace();
        }
        return left;
    }

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

                double l = left.asDouble();
                double r = right.asDouble();
                boolean result = switch (op) {
                    case "<" -> l < r;
                    case ">" -> l > r;
                    case "<=" -> l <= r;
                    case ">=" -> l >= r;
                    default -> false;
                };
                left = Value.of(result);
            } else {
                break;
            }
            skipWhitespace();
        }
        return left;
    }

    private Value parseAdditive() {
        Value left = parseMultiplicative();
        skipWhitespace();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '+' || c == '-') {
                pos++;
                skipWhitespace();
                Value right = parseMultiplicative();
                if (c == '+') {
                    if (left.getValue() instanceof String || right.getValue() instanceof String) {
                        left = Value.of(left.toString() + right.toString());
                    } else {
                        left = Value.of(left.asDouble() + right.asDouble());
                    }
                } else {
                    left = Value.of(left.asDouble() - right.asDouble());
                }
            } else {
                break;
            }
            skipWhitespace();
        }
        return left;
    }

    private Value parseMultiplicative() {
        Value left = parseUnary();
        skipWhitespace();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '*' || c == '/' || c == '%') {
                pos++;
                skipWhitespace();
                Value right = parseUnary();
                double l = left.asDouble();
                double r = right.asDouble();
                double res = switch (c) {
                    case '*' -> l * r;
                    case '/' -> r != 0 ? l / r : 0;
                    case '%' -> r != 0 ? ((l % r) + r) % r : 0;
                    default -> 0;
                };
                left = Value.of(res);
            } else {
                break;
            }
            skipWhitespace();
        }
        return left;
    }

    private Value parseUnary() {
        checkDepth();
        try {
            skipWhitespace();
            if (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '!') {
                    pos++;
                    return Value.of(!parseUnary().asBoolean());
                }
                if (c == '-') {
                    pos++;
                    return Value.of(-parseUnary().asDouble());
                }
                
                // typeof operator support
                if (input.substring(pos).startsWith("typeof ")) {
                    pos += 7;
                    Value val = parseUnary();
                    if (val == Value.NULL) return Value.of("undefined");
                    if (val.isNumber()) return Value.of("number");
                    if (val.isString()) return Value.of("string");
                    if (val.isBoolean()) return Value.of("boolean");
                    if (val.isList()) return Value.of("object");
                    if (val.isMap()) return Value.of("object");
                    if (val.isCallable()) return Value.of("function");
                    return Value.of("object");
                }
            }
            return parsePrimary();
        } finally {
            exitDepth();
        }
    }

    private Value parsePrimary() {
        skipWhitespace();
        if (pos >= input.length())
            return Value.NULL;

        Value val;
        char c = input.charAt(pos);
        if (c == '(') {
            pos++;
            val = parseTernary();
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == ')')
                pos++;
        } else if (c == '"' || c == '\'') {
            val = parseString(c);
        } else if (Character.isDigit(c)) {
            val = parseNumber();
        } else if (c == '$' || Character.isLetter(c) || c == '_') {
            val = parseIdentifier();
        } else if (c == '[') {
            val = parseListLiteral();
        } else {
            val = Value.NULL;
            pos++; // Skip unknown char to avoid infinite loop
        }

        // Handle member access and calls
        while (true) {
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == '.') {
                pos++;
                String member = parseIdentifierOnly();
                if (!member.isEmpty()) {
                    val = val.getMember(member);
                }
            } else if (pos < input.length() && input.charAt(pos) == '(') {
                pos++;
                List<Value> args = parseCallArguments();
                try {
                    val = val.call(ctx, args);
                } catch (Exception e) {
                    val = Value.NULL;
                }
            } else if (pos < input.length() && input.charAt(pos) == '[') {
                pos++;
                Value index = parseTernary();
                skipWhitespace();
                if (pos < input.length() && input.charAt(pos) == ']') {
                    pos++;
                }
                val = val.getIndex(index);
            } else {
                break;
            }
        }
        return val;
    }

    private String parseIdentifierOnly() {
        skipWhitespace();
        int start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_'))
            pos++;
        return input.substring(start, pos);
    }

    private List<Value> parseCallArguments() {
        java.util.List<Value> args = new java.util.ArrayList<>();
        skipWhitespace();
        if (pos < input.length() && input.charAt(pos) == ')') {
            pos++;
            return args;
        }
        while (pos < input.length()) {
            args.add(parseTernary());
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == ',') {
                pos++;
                skipWhitespace();
            } else if (pos < input.length() && input.charAt(pos) == ')') {
                pos++;
                break;
            } else {
                break;
            }
        }
        return args;
    }

    private Value parseString(char quote) {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && input.charAt(pos) != quote) {
            if (input.charAt(pos) == '\\' && pos + 1 < input.length()) {
                pos++;
                char esc = input.charAt(pos);
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(esc);
                }
            } else {
                sb.append(input.charAt(pos));
            }
            pos++;
        }
        if (pos < input.length())
            pos++;
        return Value.of(sb.toString());
    }

    private Value parseNumber() {
        int start = pos;
        boolean hasDot = false;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' && !hasDot) {
                hasDot = true;
                pos++;
            } else {
                break;
            }
        }
        try {
            return Value.of(Double.parseDouble(input.substring(start, pos)));
        } catch (NumberFormatException e) {
            return Value.of(0);
        }
    }

    private Value parseIdentifier() {
        int start = pos;
        boolean isEnv = input.charAt(pos) == '$';
        if (isEnv)
            pos++;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_'))
            pos++;
        String id = input.substring(isEnv ? start + 1 : start, pos);

        if (!isEnv) {
            if (id.equals("true"))
                return Value.TRUE;
            if (id.equals("false"))
                return Value.FALSE;
            if (id.equals("null"))
                return Value.NULL;
        }

        return ctx.getVariable(isEnv ? "$" + id : id);
    }

    private Value parseListLiteral() {
        pos++; // [
        java.util.List<Value> list = new java.util.ArrayList<>();
        skipWhitespace();
        if (pos < input.length() && input.charAt(pos) == ']') {
            pos++;
            return Value.of(list);
        }
        while (pos < input.length()) {
            list.add(parseTernary());
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == ',') {
                pos++;
                skipWhitespace();
            } else if (pos < input.length() && input.charAt(pos) == ']') {
                pos++;
                break;
            } else {
                break;
            }
        }
        return Value.of(list);
    }

    private void skipWhitespace() {
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }
            if (c == '/' && pos + 1 < input.length()) {
                char next = input.charAt(pos + 1);
                if (next == '/') {
                    pos = input.length();
                    return;
                }
                if (next == '*') {
                    pos += 2;
                    while (pos + 1 < input.length()) {
                        if (input.charAt(pos) == '*' && input.charAt(pos + 1) == '/') {
                            pos += 2;
                            break;
                        }
                        pos++;
                    }
                    continue;
                }
            }
            break;
        }
    }

    public static Value evaluate(String expression, Context ctx) {
        return new Parser(expression, ctx).parse();
    }
}
