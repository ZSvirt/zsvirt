package org.zstack.core.aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public aspect AsyncBackupAspect {
    private final CLogger logger = Utils.getLogger(AsyncBackupAspect.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private boolean isAsyncBackup(Object backup) {
        return backup instanceof Message
                || backup instanceof Completion
                || backup instanceof ReturnValueCompletion
                || backup instanceof AsyncLatch
                || backup instanceof FlowTrigger
                || backup instanceof SyncTaskChain
                || backup instanceof NoErrorCompletion
                || backup instanceof WhileDoneCompletion
                || backup instanceof HaCheckerCompletion
                || backup instanceof FlowRollback
                || backup instanceof ChainTask;
    }


    private void backup(List<AsyncBackup> ancestors, Throwable t, boolean flipThrowable) {
        if (ancestors == null || ancestors.isEmpty()) {
            // throw original exception out if no backup specified
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            } else if (t instanceof Error) {
                throw (Error)t;
            } else {
                throw new CloudRuntimeException(t);
            }
        }

        for (AsyncBackup ancestor : ancestors) {
            if (!isAsyncBackup(ancestor)) {
                String info = String.format("%s is not a known AsyncBackup. Someone added this type but forgot changing AsyncBackupAspect, please file bug", ancestor.getClass().getName());
                DebugUtils.dumpStackTrace(info);
                return;
            }

            ErrorCode err = t instanceof OperationFailureException ? ((OperationFailureException) t).getErrorCode() : errf.throwableToInternalError(t);
            if (ancestor instanceof Completion) {
                ((Completion)ancestor).fail(err);
            } else if (ancestor instanceof ReturnValueCompletion) {
                ((ReturnValueCompletion)ancestor).fail(err);
            } else if (ancestor instanceof AsyncLatch) {
                ((AsyncLatch) ancestor).ack();
            } else if (ancestor instanceof FlowTrigger) {
                ((FlowTrigger) ancestor).fail(err);
            } else if (ancestor instanceof FlowRollback) {
                ((FlowRollback) ancestor).rollback();
            } else if (ancestor instanceof SyncTaskChain) {
                ((SyncTaskChain) ancestor).next();
            } else if (ancestor instanceof HaCheckerCompletion) {
                ((HaCheckerCompletion) ancestor).noWay();
            } else if (ancestor instanceof NoErrorCompletion) {
                if (ancestor instanceof WhileCompletion && !((WhileCompletion) ancestor).getAddErrorCalled().get()) {
                    ((WhileCompletion) ancestor).addError(err);
                }
                ((NoErrorCompletion) ancestor).done();
            } else if (ancestor instanceof WhileDoneCompletion) {
                ErrorCodeList errs = new ErrorCodeList();
                errs.getCauses().add(err);
                ((WhileDoneCompletion) ancestor).done(errs);
            } else if (ancestor instanceof Message) {
                bus.logExceptionWithMessageDump((Message) ancestor, t);
                bus.replyErrorByMessageType((Message) ancestor, err);
            } else {
                throw new CloudRuntimeException("should not be here");
            }
        }

        if (flipThrowable && t instanceof RuntimeException) {
            throw (RuntimeException)t;
        } else {
            logger.warn("unhandled exception happened", t);
        }
    }

    private void backup(List<AsyncBackup> ancestors, Throwable t) {
        backup(ancestors, t, false);
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.Completion+.success()) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.Completion+.fail(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.ReturnValueCompletion+.success(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.ReturnValueCompletion+.fail(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.NoErrorCompletion+.done()) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.WhileCompletion+.allDone()) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.WhileDoneCompletion+.done(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.NopeCompletion+.success()) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.NopeCompletion+.fail(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.core.cloudbus.CloudBusCallBack+.run(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.core.cloudbus.CloudBusListCallBack+.run(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.core.thread.ChainTask+.run(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t, true);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.rest.AsyncRESTCallback+.timeout(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.rest.AsyncRESTCallback+.success(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.rest.AsyncRESTCallback+.fail(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.workflow.FlowDoneHandler+.handle(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.workflow.FlowErrorHandler+.handle(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }

    void around(org.zstack.header.core.AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.workflow.FlowFinallyHandler+.Finally(..)) {
        try {
            proceed(completion);
        } catch (Throwable  t) {
            backup(completion.getBackups(), t);
        }
    }
}
