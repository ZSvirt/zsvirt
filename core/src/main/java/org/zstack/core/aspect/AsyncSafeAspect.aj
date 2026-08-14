package org.zstack.core.aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.*;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public aspect AsyncSafeAspect {
    private static final CLogger logger = Utils.getLogger(AsyncSafeAspect.class);

    pointcut asyncSafe1() : execution(* *.*(.., Completion, ..));
    pointcut asyncSafe2() : execution(* *.*(.., NoErrorCompletion+, ..));
    pointcut asyncSafe3() : execution(* *.*(.., ReturnValueCompletion, ..));
    pointcut asyncSafe4() : execution(* *.*(.., WhileDoneCompletion, ..));
    pointcut asyncSafe5() : execution(* *.*(.., HaCheckerCompletion, ..));

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;

    interface Wrapper {
        void call(ErrorCode err);
    }

    private List<Wrapper> getAsyncInterface(Object[] args) {
        List<Wrapper> wrappers = new ArrayList<Wrapper>();
        for (final Object arg : args) {
            Wrapper w = null;
            if (arg instanceof Completion) {
                w = new Wrapper() {
                    @Override
                    public void call(ErrorCode err) {
                        Completion completion = (Completion)arg;
                        completion.fail(err);
                    }
                };
            } else if (arg instanceof ReturnValueCompletion) {
                w = new Wrapper() {
                    @Override
                    public void call(ErrorCode err) {
                        ReturnValueCompletion completion = (ReturnValueCompletion) arg;
                        completion.fail(err);
                    }
                };
            } else if (arg instanceof NoErrorCompletion) {
                w = new Wrapper() {
                    @Override
                    public void call(ErrorCode err) {
                        NoErrorCompletion completion = (NoErrorCompletion)arg;
                        if (completion instanceof WhileCompletion && !((WhileCompletion) completion).getAddErrorCalled().get()) {
                            ((WhileCompletion) completion).addError(err);
                        }
                        completion.done();
                    }
                };
            } else if (arg instanceof WhileDoneCompletion) {
                w = err -> {
                    ErrorCodeList errs = new ErrorCodeList();
                    errs.getCauses().add(err);
                    WhileDoneCompletion completion = (WhileDoneCompletion)arg;
                    completion.done(errs);
                };
            } else if (arg instanceof HaCheckerCompletion) {
                w = err -> ((HaCheckerCompletion) arg).noWay();
            } else if (arg instanceof Message) {
                w = new Wrapper() {
                    @Override
                    public void call(ErrorCode err) {
                        Message msg = (Message) arg;
                        bus.replyErrorByMessageType(msg, err);
                    }
                };
            }

            if (w != null) {
                wrappers.add(w);
            }
        }


        return wrappers;
    }

    Object around() : asyncSafe1() || asyncSafe2() || asyncSafe3() || asyncSafe4() || asyncSafe5() {
        try {
            return proceed();
        } catch (Throwable t) {
            List<Wrapper> wrappers = getAsyncInterface(thisJoinPoint.getArgs());
            if (wrappers.isEmpty()) {
                String err = String.format(
                        "%s has triggered async safe aspectj, however, it has neither Completion nor ReturnValueCompletion in its method arguments", thisJoinPoint
                                .getSignature().toLongString());
                throw new CloudRuntimeException(err, t);
            }

            ErrorCode errCode = null;
            if (t instanceof OperationFailureException) {
                errCode = ((OperationFailureException) t).getErrorCode();
            } else {
                String err = String.format("unhandled exception happened when calling %s, %s", thisJoinPoint.getSignature().toLongString(), t.getMessage());
                errCode = errf.stringToInternalError(err);
                logger.warn(err, t);
            }

            for (Wrapper w : wrappers) {
                w.call(errCode);
            }

            return null;
        }
    }
}
