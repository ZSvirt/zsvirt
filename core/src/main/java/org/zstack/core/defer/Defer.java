package org.zstack.core.defer;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

public class Defer {
    private static ThreadLocal<Map<String, DeferStack<Runnable>>> stack = new ThreadLocal<Map<String, DeferStack<Runnable>>>();
    private static final CLogger logger = Utils.getLogger(Defer.class);

    private static final String EXCEPTION_STACK = "exception";
    private static final String NON_EXCEPTION_STACK = "nonException";

    private static DeferStack<Runnable> getExceptionStack()  {
        Map<String, DeferStack<Runnable>> s = stack.get();
        if (s == null) {
            s = new HashMap<String, DeferStack<Runnable>>();
            stack.set(s);
        }

        DeferStack<Runnable> sg = s.get(EXCEPTION_STACK);
        if (sg == null) {
            sg = new DeferStack<Runnable>();
            s.put(EXCEPTION_STACK, sg);
        }
        return sg;
    }

    private static DeferStack<Runnable> getNonExceptionStack()  {
        Map<String, DeferStack<Runnable>> s = stack.get();
        if (s == null) {
            s = new HashMap<String, DeferStack<Runnable>>();
            stack.set(s);
        }

        DeferStack<Runnable> sg = s.get(NON_EXCEPTION_STACK);
        if (sg == null) {
            sg = new DeferStack<Runnable>();
            s.put(NON_EXCEPTION_STACK, sg);
        }
        return sg;
    }
    
    public static void guard(Runnable r) {
        DeferStack<Runnable> sg = getExceptionStack();
        sg.push(r);
    }

    public static void defer(Runnable r) {
        DeferStack<Runnable> sg = getNonExceptionStack();
        sg.push(r);
    }

    static void runDefer(int top) {
        DeferStack<Runnable> sg = getNonExceptionStack();
        int count = sg.size() - top;
        for (int i=0; i<count; i++) {
            try {
                Runnable r = sg.pop();
                r.run();
            } catch (Throwable e) {
                logger.warn("a unhandled exception happened", e);
            }
        }
    }

    static void pushNonExceptionTop() {
        DeferStack<Runnable> sg = getNonExceptionStack();
        sg.pushTop();
    }

    static int popNonExceptionTop() {
        DeferStack<Runnable> sg = getNonExceptionStack();
        return sg.popTop();
    }

    static int getNonExceptionStackTop() {
        DeferStack<Runnable> sg = getNonExceptionStack();
        return sg.getTop();
    }

    static int getNonExceptionStackSize() {
        DeferStack<Runnable> sg = getNonExceptionStack();
        return sg.size();
    }

    static void pushExceptionTop() {
        DeferStack<Runnable> sg = getExceptionStack();
        sg.pushTop();
    }
    
    static int popExceptionTop() {
        DeferStack<Runnable> sg = getExceptionStack();
        return sg.popTop();
    }
    
    static void rollbackExceptionStack(String sourceLocation, int top) {
        DeferStack<Runnable> sg = getExceptionStack();
        int count = sg.size() - top;
        for (int i=0; i<count; i++) {
            try {
                Runnable r = sg.pop();
                r.run();
            } catch (Throwable e) {
                logger.warn("A unhandled exception happened during calling defer in " + sourceLocation, e);
            }
        }
    }
    
    static void revertExceptionStack(int top) {
        DeferStack<Runnable> sg = getExceptionStack();
        sg.revert(top);
    }
    
    static int getExceptionStackTop() {
        DeferStack<Runnable> sg = getExceptionStack();
        return sg.getTop();
    }
    
    static int getExceptionStackSize() {
        DeferStack<Runnable> sg = getExceptionStack();
        return sg.size();
    }
}
