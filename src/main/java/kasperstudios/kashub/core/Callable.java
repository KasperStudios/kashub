package kasperstudios.kashub.core;

import java.util.List;

/**
 * Callable - Interface for anything that can be called as a function in the
 * engine.
 */
public interface Callable {
    /**
     * Executes the call logic.
     * 
     * @param ctx  Execution context
     * @param args List of arguments
     * @return Result of the call
     * @throws Exception if execution fails
     */
    Value call(Context ctx, List<Value> args) throws Exception;
}