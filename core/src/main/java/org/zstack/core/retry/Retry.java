package org.zstack.core.retry;

import org.zstack.header.Confirm;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.i18n;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.zstack.header.Confirm.*;

/**
 * Created by xing5 on 2016/6/25.
 */
public abstract class Retry<T> {
    public static class RetryException extends RuntimeException {
        public RetryException() {
        }

        public RetryException(String message) {
            super(message);
        }

        public RetryException(String message, Throwable cause) {
            super(message, cause);
        }

        public RetryException(Throwable cause) {
            super(cause);
        }

        public RetryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    protected int times, count = 5;
    protected int interval = 1;
    protected final List<Function<Throwable, Confirm>> retryConditions = new ArrayList<>();

    private static final CLogger logger = Utils.getLogger(Retry.class);

    protected abstract T call();

    // return true to continue to throw out the exception
    protected boolean onFailure(Throwable t) {
        return true;
    }

    protected String __name__;

    public T run() {
        Method m;
        try {
            m = getClass().getDeclaredMethod("call");
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        RetryCondition cond = m.getAnnotation(RetryCondition.class);
        if (cond != null) {
            times = cond.times();
            interval = cond.interval();
            Class<?>[] onExceptions = cond.onExceptions();
            retryConditions.add(e -> onExceptions.length != 0 && !TypeUtils.isTypeOf(e, onExceptions) ? No : Yes);
        }

        count = times;

        if (__name__ == null) {
            __name__ = getClass().getName();
        }

        do {
            try {
                return call();
            } catch (Throwable t) {
                Confirm retry = Skip;
                for (Function<Throwable, Confirm> condition : retryConditions) {
                    retry = condition.apply(t);
                    if (retry != Skip) {
                        break;
                    }
                }

                if (retry == No) {
                    throw t;
                }

                try {
                    TimeUnit.SECONDS.sleep(interval);
                } catch (InterruptedException e) {
                    logger.warn(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }


                logger.debug(String.format("running [%s] encounters an exception[%s], will retry %s times with the" +
                        " interval[%s]", __name__, t.getMessage(), count, interval));

                count --;

                if (count <= 0) {
                    ErrorCode errorCode = new ErrorCode();
                    errorCode.setCode(SysErrors.OPERATION_ERROR.toString());
                    errorCode.setDescription(i18n("an operation[%s] fails after retrying %s times with the interval %s seconds",
                            __name__, times, interval));
                    errorCode.setDetails(t.getMessage());
                    logger.warn(errorCode.getReadableDetails(), t);

                    if (onFailure(t)) {
                        throw t;
                    }

                    return null;
                }
            }
        } while (true);
    }

    public void stop(){
        count = 0;
    }
}
