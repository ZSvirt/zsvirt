package org.zstack.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskContext {
    private static final ConcurrentHashMap<Long, ConcurrentHashMap<Object, Object>> taskContexts = new ConcurrentHashMap<>();

    public static Map<Object, Object> getOrNewTaskContext() {
        return taskContexts.computeIfAbsent(Thread.currentThread().getId(), x -> new ConcurrentHashMap<>());
    }

    public static Map<Object, Object> getTaskContext() {
        return taskContexts.get(Thread.currentThread().getId());
    }

    public static void setTaskContext(Map<Object, Object> ctx) {
        taskContexts.put(Thread.currentThread().getId(), new ConcurrentHashMap<>(ctx));
    }

    public static boolean containsTaskContext(Object key) {
        Map<Object, Object> ctx = getTaskContext();
        if (ctx != null) {
            return ctx.containsKey(key);
        }

        return false;
    }

    public static void removeTaskContext(long threadID) {
        taskContexts.remove(threadID);
    }

    public static void removeTaskContext() {
        taskContexts.remove(Thread.currentThread().getId());
    }

    public static void putTaskContextItem(Object key, Object value) {
        getOrNewTaskContext().put(key, value);
    }

    public static Object getTaskContextItem(Object key) {
        Map<Object, Object> ctx = getTaskContext();
        if (ctx != null) {
            return ctx.get(key);
        }

        return null;
    }

    public static void removeTaskContextItem(Object key) {
        Map<Object, Object> ctx = taskContexts.get(Thread.currentThread().getId());
        if (ctx != null) {
            ctx.remove(key);
        }
    }
}
