package org.zstack.core;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.inerr;
/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class With {
    private static final CLogger logger = Utils.getLogger(With.class);
    private Message[] msgs;
    private AsyncBackup backup;
    
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    public With(AsyncBackup backup, Message...msgs) {
        this.backup = backup;
        this.msgs = msgs;
        check();
    }

    public With(AsyncBackup backup) {
        this.backup = backup;
        check();
    }

    public With(Message...msgs) {
        this.msgs = msgs;
    }

    private void backup(Throwable t) {
        if (!(backup instanceof Message)) {
            logger.warn(String.format("unhandled exception happened"), t);
        }

        ErrorCode err = inerr(t.getMessage());
        if (backup instanceof Completion) {
            ((Completion)backup).fail(err);
        } else if (backup instanceof ReturnValueCompletion) {
            ((ReturnValueCompletion)backup).fail(err);
        } else if (backup instanceof FlowTrigger) {
            ((FlowTrigger) backup).fail(err);
        } else if (backup instanceof  SyncTaskChain) {
            ((SyncTaskChain) backup).next();
        } else if (backup instanceof NoErrorCompletion) {
            ((NoErrorCompletion) backup).done();
        } else if (backup instanceof Message) {
            bus.logExceptionWithMessageDump((Message) backup, t);
            bus.replyErrorByMessageType((Message) backup, err);
        } else {
            throw new CloudRuntimeException("should not be here");
        }

        if (msgs != null) {
            for (Message msg : msgs) {
                bus.logExceptionWithMessageDump(msg, t);
                bus.replyErrorByMessageType(msg, err);
            }
        }
    }

    private void check() {
        if (backup == null) {
            return;
        }

        if (!(
                backup instanceof Completion || backup instanceof ReturnValueCompletion || backup instanceof Message
                        || backup instanceof FlowTrigger || backup instanceof SyncTaskChain || backup instanceof NoErrorCompletion
        )) {
            String info = String.format("%s is not a known AsyncBackup. Someone added this type but forgot changing AsyncBackupAspect, please file bug", backup.getClass().getName());
            throw new CloudRuntimeException(info);
        }

    }

    public void run(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            backup(t);
        }
    }

    public void runWithException(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException re) {
            backup(re);
            throw re;
        } catch (Error err) {
            backup(err);
            throw err;
        }
    }
}
