package org.zstack.core.defer;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public aspect DeferAspect {
    private static final CLogger logger = Utils.getLogger(DeferAspect.class);

    pointcut deferCalled() : execution(@org.zstack.core.defer.Deferred * *.*(..));
    
    declare error: call(void org.zstack.core.defer.Defer.guard(..)) && !withincode(@org.zstack.core.defer.Deferred * *.*(..)) : "Defer.guard() must be called in method with @Deferred annotation";
    declare error: call(void org.zstack.core.defer.Defer.defer(..)) && !withincode(@org.zstack.core.defer.Deferred * *.*(..)) : "Defer.defer() must be called in method with @Deferred annotation";

    before() : deferCalled() {
        Defer.pushExceptionTop();
        Defer.pushNonExceptionTop();
        String methodName = "[" + thisJoinPoint.getSignature().toLongString() + "]";
        String msg = "Saving defer stack pointer for " + methodName + ". Current stack top at: " + Defer.getExceptionStackTop() + ". Current stack size: "
                + Defer.getExceptionStackSize();
        logger.trace(msg);
    }

    private boolean isInstance(Class<? extends Throwable>[] arr, Throwable t) {
        for (Class<? extends Throwable> i : arr) {
           if (i.isInstance(t)) {
               return true;
           }
        }
        return false;
    }
    
    private boolean needRollback(JoinPoint point, Throwable t) {
        MethodSignature mtd = (MethodSignature) point.getStaticPart().getSignature();
        final Deferred at = mtd.getMethod().getAnnotation(Deferred.class);

        if (at.rollbackForClass().length != 0) {
            if (isInstance(at.rollbackForClass(), t)) {
                return true;
            } else {
                return false;
            }
        }

        if (at.noRollbackForClass().length != 0 && isInstance(at.noRollbackForClass(), t)) {
            return false;
        }

        return true;
    }

    after() throwing(Throwable t) : deferCalled() {
        if (needRollback(thisJoinPoint, t)) {
            int rollbackNum = Defer.getExceptionStackSize() - Defer.getExceptionStackTop();
            String methodName = "[" + thisJoinPoint.getSignature().toLongString() + "]";
            Defer.rollbackExceptionStack(methodName, Defer.popExceptionTop());
            String msg = methodName + " throws out a throwable(" + t.getClass().getCanonicalName() + ")." + " Having rolled back " + rollbackNum
                    + " defer. " + "Next rollback/revert will cause stack top back to: " + Defer.getExceptionStackTop() + ". Current stack size: "
                    + Defer.getExceptionStackSize();
            logger.trace(msg);
        } else {
            revert(thisJoinPoint);
        }
    }

    private void revert(JoinPoint point) {
        int revertNum = Defer.getExceptionStackSize() - Defer.getExceptionStackTop();
        Defer.revertExceptionStack(Defer.popExceptionTop());
        String methodName = "[" + point.getSignature().toLongString() + "]";
        String msg = "Having reverted " + revertNum + " defer stack pointer for " + methodName + ". Next rollback/revert will cause stack top back to: "
                + Defer.getExceptionStackTop() + ". Current stack size: " + Defer.getExceptionStackSize();
        logger.trace(msg);
    }

    after() returning : deferCalled() {
        revert(thisJoinPoint);
    }

    after() : deferCalled() {
        Defer.runDefer(Defer.popNonExceptionTop());
    }
}
