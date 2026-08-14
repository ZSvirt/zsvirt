package org.zstack.core.aspect;

import org.zstack.core.thread.Task;
import org.zstack.header.HasThreadContext;
import org.apache.logging.log4j.ThreadContext;
import org.zstack.utils.TaskContext;

import org.zstack.header.core.*;
import org.zstack.header.core.workflow.FlowRollback;

public aspect SetThreadContextAspect {
    private void setThreadContext(Object obj) {
        HasThreadContext tc = (HasThreadContext)obj;
        if (tc.threadContext != null) {
            ThreadContext.clearMap();
            ThreadContext.putAll(tc.threadContext);
        } else {
            ThreadContext.clearMap();
        }
        if (tc.threadContextStack != null) {
            ThreadContext.clearStack();
            ThreadContext.setStack(tc.threadContextStack);
        } else {
            ThreadContext.clearStack();
        }

        if (tc.taskContext != null) {
            TaskContext.setTaskContext(tc.taskContext);
        } else {
            TaskContext.removeTaskContext();
        }
    }

    before(Task task) : target(task) && execution(* Task+.call()) {
        setThreadContext(task);
    }

    before(org.zstack.header.core.Completion c) : target(c) && execution(* org.zstack.header.core.Completion+.success()) {
        setThreadContext(c);
    }

    before(org.zstack.header.core.Completion c) : target(c) && execution(* org.zstack.header.core.Completion+.fail(..)) {
        setThreadContext(c);
    }

    before(org.zstack.header.core.ReturnValueCompletion c) : target(c) && execution(* org.zstack.header.core.ReturnValueCompletion+.success(..)) {
        setThreadContext(c);
    }

    before(org.zstack.header.core.ReturnValueCompletion c) : target(c) && execution(* org.zstack.header.core.ReturnValueCompletion+.fail(..)) {
        setThreadContext(c);
    }

    before(org.zstack.header.core.NoErrorCompletion c) : target(c) && execution(* org.zstack.header.core.NoErrorCompletion+.done()) {
        setThreadContext(c);
    }

    before(org.zstack.header.core.NopeCompletion c) : target(c) && execution(* org.zstack.header.core.NopeCompletion+.success()) {
        setThreadContext(c);
    }

    before(org.zstack.header.core.NopeCompletion c) : target(c) && execution(* org.zstack.header.core.NopeCompletion+.fail(..)) {
        setThreadContext(c);
    }

    before(org.zstack.core.cloudbus.CloudBusCallBack c) : target(c) && execution(* org.zstack.core.cloudbus.CloudBusCallBack+.run(..)) {
        setThreadContext(c);
    }

    before(org.zstack.core.cloudbus.CloudBusListCallBack c) : target(c) && execution(* org.zstack.core.cloudbus.CloudBusListCallBack+.run(..)) {
        setThreadContext(c);
    }

    before(org.zstack.core.thread.ChainTask c) : target(c) && execution(* org.zstack.core.thread.ChainTask+.run(..)) {
        setThreadContext(c);
    }

    before(org.zstack.header.rest.AsyncRESTCallback c) : target(c) && execution(* org.zstack.header.rest.AsyncRESTCallback+.timeout(..)) {
        setThreadContext(c);
    }

    before(org.zstack.header.rest.AsyncRESTCallback c) : target(c) && execution(* org.zstack.header.rest.AsyncRESTCallback+.success(..)) {
        setThreadContext(c);
    }

    before(org.zstack.header.rest.AsyncRESTCallback c) : target(c) && execution(* org.zstack.header.rest.AsyncRESTCallback+.fail(..)) {
        setThreadContext(c);
    }

    before(org.zstack.header.core.workflow.FlowDoneHandler c) : target(c) && execution(* org.zstack.header.core.workflow.FlowDoneHandler+.handle(..)) {
        setThreadContext(c);
    }

    before(org.zstack.header.core.workflow.FlowErrorHandler c) : target(c) && execution(* org.zstack.header.core.workflow.FlowErrorHandler+.handle(..)) {
        setThreadContext(c);
    }

    before(org.zstack.header.core.workflow.FlowFinallyHandler c) : target(c) && execution(* org.zstack.header.core.workflow.FlowFinallyHandler+.Finally(..)) {
        setThreadContext(c);
    }
}