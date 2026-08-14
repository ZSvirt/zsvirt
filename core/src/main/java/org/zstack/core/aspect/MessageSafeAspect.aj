package org.zstack.core.aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;

/**
 */
public aspect MessageSafeAspect {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    after() throwing(Throwable t) : execution(@org.zstack.core.cloudbus.MessageSafe * *.*(.., Message+, ..)) {
        for (Object arg : thisJoinPoint.getArgs()) {
            if (arg instanceof Message) {
                ErrorCode err = null;
                if (t instanceof OperationFailureException) {
                    err = ((OperationFailureException)t).getErrorCode();
                } else {
                    err = errf.throwableToInternalError(t);
                }

                bus.logExceptionWithMessageDump((Message)arg, t);
                bus.replyErrorByMessageType((Message)arg, err);
            }
        }
    }
}
