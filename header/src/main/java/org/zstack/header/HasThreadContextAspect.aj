package org.zstack.header;

import org.apache.logging.log4j.ThreadContext;
import java.util.*;
import org.zstack.utils.TaskContext;

public aspect HasThreadContextAspect {
    public Map<String, String> HasThreadContext.threadContext;
    public List<String> HasThreadContext.threadContextStack;
    public Map<Object, Object> HasThreadContext.taskContext;

    after(HasThreadContext obj) : target(obj) && execution(HasThreadContext+.new(..)) {
        setThreadContext(obj);
    }

    Map<String, String> around(HasThreadContext context) : target(context) && execution(Map<String, String> HasThreadContext+.getThreadContext()) {
        return context.threadContext;
    }

    Map<Object, Object> around(HasThreadContext context) : target(context) && execution(Map<Object, Object> HasThreadContext+.getTaskContext()) {
        return context.taskContext;
    }

    public static void setThreadContext(HasThreadContext obj) {
        obj.threadContext = ThreadContext.getContext();
        obj.threadContextStack = ThreadContext.getImmutableStack().asList();
        // store copy of context not ref
        obj.taskContext = TaskContext.getTaskContext() != null ? new HashMap<>(TaskContext.getTaskContext()) : null;
    }
}