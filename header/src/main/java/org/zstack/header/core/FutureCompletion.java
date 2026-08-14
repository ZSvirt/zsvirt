package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.concurrent.TimeUnit;

/**
 */
public class FutureCompletion extends Completion {
    private volatile boolean success;
    private ErrorCode errorCode;
    private volatile boolean done;
    private boolean timeout;

    public boolean isTimeout() {
        return timeout;
    }

    public void setTimeout(boolean timeout) {
        this.timeout = timeout;
    }

    public FutureCompletion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    @Override
    public final synchronized void success() {
        this.success = true;
        done = true;
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

    public synchronized void await(long timeout) {
        if (done) {
            return;
        }

        doWait(timeout);

        if (!done) {
            this.timeout = true;
            ErrorCode err = new ErrorCode();
            err.setCode(SysErrors.TIMEOUT.toString());
            err.setDetails(String.format("FutureCompletion timeout after %s seconds", TimeUnit.MILLISECONDS.toSeconds(timeout)));
            fail(err);
        }
    }

    public synchronized boolean tryWait(long timeout) {
        if (done) {
            return done;
        }

        doWait(timeout);

        if (!done) {
            this.timeout = true;
        }

        return done;
    }

    private synchronized void doWait(long timeout) {
        try {
            wait(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException(e);
        }
    }

    public synchronized void await() {
        if (done) {
            return;
        }

        try {
            wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException(e);
        }
    }
}
