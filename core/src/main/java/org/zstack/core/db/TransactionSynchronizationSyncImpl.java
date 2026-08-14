package org.zstack.core.db;

import org.springframework.transaction.support.TransactionSynchronization;
import org.zstack.core.db.TransactionalCallback.Operation;

public class TransactionSynchronizationSyncImpl implements TransactionSynchronization {
    private final Class<?>[] clazzs;
    private final TransactionalSyncCallback callback;
    private final Operation op;
    
    TransactionSynchronizationSyncImpl(TransactionalSyncCallback cb, Operation op, Class<?>...clazzs) {
        this.clazzs = clazzs;
        this.callback = cb;
        this.op = op;
    }
    
    @Override
    public void suspend() {
        callback.suspend(clazzs);
    }

    @Override
    public void resume() {
        callback.resume(clazzs);
    }

    @Override
    public void flush() {
        callback.flush(clazzs);
    }

    @Override
    public void beforeCommit(boolean readOnly) {
        callback.beforeCommit(op, readOnly, clazzs);
    }

    @Override
    public void beforeCompletion() {
        callback.beforeCompletion(op, clazzs);
    }

    @Override
    public void afterCommit() {
        callback.afterCommit(op, clazzs);
    }

    @Override
    public void afterCompletion(int status) {
        callback.afterCompletion(op, status, clazzs);
    }
}
