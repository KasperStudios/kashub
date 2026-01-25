package kasperstudios.kashub.core;

import java.util.List;
import java.util.Objects;

/**
 * ExpressionParser - Enhanced expression parser for the script engine.
 * Directly uses ScriptValue and ScriptContext for efficient evaluation.
 * Supports arithmetic, logic, comparison, and ternary operators.
 */
public class ExpressionParser {
    private final String input;
    private int pos;
    private final Context ctx;

    public ExpressionParser(String input, Context ctx) {
        this.input = input.trim();
        this.ctx = ctx;
        this.pos = 0;
    }

    public Value parse() {
        skipWhitespace();
        if (pos >= input.length())
            return Value.NULL;
        Value result = parseTernary();
        skipWhitespace();
        return result;
    }

    private Value parseTernary() {
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
                left = Value.of(
                        Objects.equals(left.getValue(), right.getValue()) || left.toString().equals(right.toString()));
            } else if (input.charAt(pos) == '!' && input.charAt(pos + 1) == '=') {
                pos += 2;
                skipWhitespace();
                Value right = parseComparison();
                left = Value.of(!Objects.equals(left.getValue(), right.getValue())
                        && !left.toString().equals(right.toString()));
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
                    case '%' -> r != 0 ? l % r : 0;
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
        skipWhitespace();
        if (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '!') {
                pos++;
                return Value.of(!parseUnary().asBoolean());
            }
            if (c == '-' && (pos + 1 >= input.length() || !Character.isDigit(input.charAt(pos + 1)))) {
                pos++;
                return Value.of(-parseUnary().asDouble());
            }
        }
        return parsePrimary();
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
        }

        // Handle member access and calls
        while (true) {
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == '.') {
                pos++;
                String member = parseIdentifierOnly();
                val = val.getMember(member);
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
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.'))
            pos++;
        return Value.of(Double.parseDouble(input.substring(start, pos)));
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
                return Value.of(true);
            if (id.equals("false"))
                return Value.of(false);
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
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos)))
            pos++;
    }

    public static Value evaluate(String expression, Context ctx) {
        return new ExpressionParser(expression, ctx).parse();
    }
}