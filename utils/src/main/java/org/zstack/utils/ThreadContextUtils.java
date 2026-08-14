package org.zstack.utils;

import org.apache.logging.log4j.ThreadContext;

import java.util.HashMap;
import java.util.Map;

public class ThreadContextUtils {
    public static Runnable saveThreadContext() {
        ThreadContextMapSaved savedThread = new ThreadContextMapSaved();
        savedThread.contextMap = ThreadContext.getContext();
        savedThread.contextStack = ThreadContext.cloneStack();

        return () -> {
            ThreadContext.clearAll();
            ThreadContext.putAll(savedThread.contextMap);
            ThreadContext.setStack(savedThread.contextStack.asList());
        };
    }


    private static class ThreadContextMapSaved {
        Map<String, String> contextMap = new HashMap<>();
        ThreadContext.ContextStack contextStack;
    }
}
