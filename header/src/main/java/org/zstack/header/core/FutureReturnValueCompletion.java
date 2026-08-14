package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 */
public class FutureReturnValueCompletion extends ReturnValueCompletion {
    private static final CLogger logger = Utils.getLogger(FutureReturnValueCompletion.class);

    private volatile boolean success;
    private ErrorCode errorCode;
    private volatile boolean done;
    private Object result;
    private long SLOW_FUTURE_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

    public FutureReturnValueCompletion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    @Override
    public final synchronized void success(Object returnValue) {
        this.success = true;
        done = true;
        result = returnValue;
        notifyAll();
    }

    @Override
    public synchronized void fail(ErrorCode errorCode) {
        this.success = false;
        this.errorCode = errorCode;
        done = true;
        notifyAll();
    }

    public boolean isSuccess() {
        return success;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private synchronized void dumpSlowFuture() {
        try {
            wait(SLOW_FUTURE_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException(e);
        }

        try {
            if (!done) {
                AsyncBackup backup = new AsyncBackup() {};
                if (backups != null && backups.size() > 0) {
                    backup = backups.get(0);
                }

                String debugInfo = String.format("Future completion wait over %s milliseconds, detected slow future completion! async backup info: %s",
                        SLOW_FUTURE_TIMEOUT, backup.getClass().getCanonicalName());

                if (backup instanceof Message) {
                    debugInfo = debugInfo.concat(String.format("; message dump: %s", JSONObjectUtil.toJsonString((Message)backup)));
                }

                DebugUtils.dumpStackTrace(debugInfo);
            }
        } catch (Exception e) {
            logger.warn(String.format("dumpSlowFuture get exception %s %s", e.getMessage(), e.toString()));
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void await(long timeout) {
        if (done) {
            return;
        }

        if (timeout > SLOW_FUTURE_TIMEOUT) {
            timeout = timeout - SLOW_FUTURE_TIMEOUT;
            dumpSlowFuture();

            if (done) {
                return;
            }
        }

        try {
            wait(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException(e);
        }

        if (!done) {
            ErrorCode err = new ErrorCode();
            err.setCode(SysErrors.TIMEOUT.toString());
            err.setDetails(String.format("FutureCompletion timeout after %s seconds", TimeUnit.MILLISECONDS.toSeconds(timeout)));
            fail(err);
        }
    }

    public synchronized void await() {
        if (done) {
            return;
        }

        dumpSlowFuture();
        try {
            wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException(e);
        }
    }

    public <T> T getResult() {
        return (T) result;
    }
}
