package kasperstudios.kashub.debug;

public class Breakpoint {
    private final int line;
    private boolean enabled;
    private BreakpointType type;
    private String condition;
    private String logMessage;
    private int hitCount;
    private String hitCondition;

    public Breakpoint(int line) {
        this.line = line;
        this.enabled = true;
        this.type = BreakpointType.BREAKPOINT;
        this.hitCount = 0;
    }

    public int getLine() {
        return line;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BreakpointType getType() {
        return type;
    }

    public void setType(BreakpointType type) {
        this.type = type;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
        if (condition != null && !condition.trim().isEmpty()) {
            this.type = BreakpointType.CONDITIONAL;
        }
    }

    public String getLogMessage() {
        return logMessage;
    }

    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
        if (logMessage != null && !logMessage.trim().isEmpty()) {
            this.type = BreakpointType.LOGPOINT;
        }
    }

    public int getHitCount() {
        return hitCount;
    }

    public void incrementHitCount() {
        this.hitCount++;
    }

    public void resetHitCount() {
        this.hitCount = 0;
    }

    public String getHitCondition() {
        return hitCondition;
    }

    public void setHitCondition(String hitCondition) {
        this.hitCondition = hitCondition;
    }

    public boolean checkHitCondition() {
        if (hitCondition == null || hitCondition.trim().isEmpty()) {
            return true;
        }

        try {
            String condition = hitCondition.trim();
            int count = hitCount;

            if (condition.startsWith(">=")) {
                int threshold = Integer.parseInt(condition.substring(2).trim());
                return count >= threshold;
            } else if (condition.startsWith("<=")) {
                int threshold = Integer.parseInt(condition.substring(2).trim());
                return count <= threshold;
            } else if (condition.startsWith(">")) {
                int threshold = Integer.parseInt(condition.substring(1).trim());
                return count > threshold;
            } else if (condition.startsWith("<")) {
                int threshold = Integer.parseInt(condition.substring(1).trim());
                return count < threshold;
            } else if (condition.startsWith("==")) {
                int threshold = Integer.parseInt(condition.substring(2).trim());
                return count == threshold;
            } else if (condition.contains("%")) {

                String[] parts = condition.split("%");
                if (parts.length == 2) {
                    int modulo = Integer.parseInt(parts[0].trim());
                    String rest = parts[1].trim();
                    if (rest.startsWith("==")) {
                        int expected = Integer.parseInt(rest.substring(2).trim());
                        return (count % modulo) == expected;
                    }
                }
            }
        } catch (Exception e) {

        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Breakpoint{line=").append(line);
        sb.append(", type=").append(type);
        sb.append(", enabled=").append(enabled);
        if (condition != null) {
            sb.append(", condition='").append(condition).append("'");
        }
        if (logMessage != null) {
            sb.append(", logMessage='").append(logMessage).append("'");
        }
        if (hitCondition != null) {
            sb.append(", hitCondition='").append(hitCondition).append("'");
        }
        sb.append(", hitCount=").append(hitCount);
        sb.append("}");
        return sb.toString();
    }
}
