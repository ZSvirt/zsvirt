package org.zstack.core.thread;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractFuture<T> implements Future<T> {
    private static CLogger logger = Utils.getLogger(AbstractFuture.class);
    
    protected volatile Throwable exception;
    protected AtomicBoolean _done = new AtomicBoolean(false);
    protected AtomicBoolean canceled = new AtomicBoolean(false);
    protected volatile T ret;
    protected final Object task;
    
    public AbstractFuture(Object task) {
        this.task = task;
        this.exception = null;
        this.ret = null;
    }
    
    private void doWait(long timeout) throws InterruptedException, TimeoutException {
        synchronized (this) {
            while (!isDone() && !isCancelled()) {
                this.wait(timeout);
                if (isCancelled()) {
                    throw new CancellationException(task.getClass().getCanonicalName() + " has been cancelled");
                }
                
                if (!isDone()) {
                    throw new TimeoutException("Timeout after " + timeout + " milliseconds");
                }
            }
        }
    }
    
    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (isCancelled()) {
            throw new CancellationException(task.getClass().getCanonicalName() + " has been cancelled");
        }
        
        try {
            doWait(0);
        } catch (TimeoutException e) {
            // pass on purpose
        }

        if (exception != null) {
            throw new ExecutionException(exception);
        }

        return ret;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (isCancelled()) {
            throw new CancellationException(task.getClass().getCanonicalName() + " has been cancelled");
        }
        
        doWait(unit.toMillis(timeout));
        if (exception != null) {
            throw new ExecutionException(exception);
        }

        return ret;
    }

    @Override
    public boolean isCancelled() {
        return canceled.get();
    }

    @Override
    public boolean isDone() {
        return _done.get();
    }
    
    protected void done() {
        synchronized (this) {
            if (!_done.compareAndSet(false, true)) {
                return;
            }

            this.notifyAll();
        }
    }
    
    protected void cancel() {
        synchronized (this) {
            if (!canceled.compareAndSet(false, true)) {
                return;
            }

            this.notifyAll();
        }
    }

    public void finish(T result) {
        ret = result;
        done();
    }
}
