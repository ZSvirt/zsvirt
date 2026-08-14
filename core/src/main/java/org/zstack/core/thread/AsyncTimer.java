package org.zstack.core.thread;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AsyncTimer implements Runnable {
    protected static final CLogger logger = Utils.getLogger(AsyncTimer.class);
    protected static long index = 0;

    protected TimeUnit timeUnit;
    protected long period;
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private ThreadFacadeImpl.TimeoutTaskReceipt cancel;

    protected String __name__ = getClass().getName();

    @Autowired
    protected ThreadFacade thdf;

    protected long num = index++;

    protected abstract void execute();

    public AsyncTimer(TimeUnit timeUnit, long period) {
        this.timeUnit = timeUnit;
        this.period = period;
    }
    
    protected String getName() {
        return String.format("async-timer-%s[%s]", num, __name__);
    }


    protected TimeUnit getTimeUnit() {
        return timeUnit;
    }

    protected long getPeriod() {
        return period;
    }

    public void start() {
        if (cancelled.get()) {
            throw new CloudRuntimeException("cannot start a cancelled timer");
        }

        cancel = thdf.submitTimeoutTask(this, getTimeUnit(), getPeriod());
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("%s starts", getName()));
        }
    }

    public void startRightNow() {
        if (cancelled.get()) {
            throw new CloudRuntimeException("cannot start a cancelled timer");
        }

        cancel = thdf.submitTimeoutTask(this, getTimeUnit(), 0);
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("%s starts", getName()));
        }
    }

    public void cancel() {
        cancelled.set(true);
        if (cancel != null) {
            cancel.cancel();
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("%s cancelled", getName()));
        }
    }

    protected boolean isCanceled() {
        return cancelled.get();
    }

    protected void continueToRunThisTimer() {
        if (cancelled.get()) {
            return;
        }

        cancel = thdf.submitTimeoutTask(this, getTimeUnit(), getPeriod());
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("%s continues to run", getName()));
        }
    }

    @Override
    public final void run() {
        if (cancelled.get()) {
            return;
        }

        try {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("%s executes", getName()));
            }

            execute();
        } catch (Throwable t) {
            logger.warn(String.format("unhandled exception while executing %s", getName()), t);
            continueToRunThisTimer();
        }
    }
}
