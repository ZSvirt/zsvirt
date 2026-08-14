package org.zstack.utils.message;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OperationChecker {
    private final Map<String, Set<String>> states = new ConcurrentHashMap<String, Set<String>>();
    private final boolean allowedWhenHavingState;

    public OperationChecker() {
        this(true);
    }

    public OperationChecker(boolean allowedWhenHavingState) {
        this.allowedWhenHavingState = allowedWhenHavingState;
    }

    public OperationChecker addState(Enum state, String...opNames) {
        return addState(state.toString(), opNames);
    }

    public OperationChecker addState(String stateName, String...opNames) {
        for (String opName : opNames) {
            Set<String> ss = states.computeIfAbsent(opName, k -> new HashSet<String>());
            ss.add(stateName);
        }

        return this;
    }

    public boolean isOperationAllowed(String operationName, String state) {
        return isOperationAllowed(operationName, state, true);
    }

    public boolean isOperationAllowed(String operationName, String state, boolean exceptionIfNoOperation) {
        Set<String> ops = states.get(operationName);
        if (exceptionIfNoOperation) {
            if (ops == null) {
                throw new IllegalArgumentException(String.format("Unable to find allowed states for operation[%s], current state is %s", operationName, state));
            }
        } else {
            if (ops == null) {
                return true;
            }
        }
        
        return allowedWhenHavingState == ops.contains(state);
    }

    public boolean isOperationForbidden(String operationName, String state) {
        Set<String> ops = states.get(operationName);
        if (ops == null) {
            return false;
        }
        return ops.contains(state);
    }

    public Set<String> getStatesForOperation(String operationName) {
        Set<String> ops = states.get(operationName);
        if (ops == null) {
            throw new IllegalArgumentException(String.format("Unable to find allowed states for operation[%s]", operationName));
        }
        return ops;
    }
}
