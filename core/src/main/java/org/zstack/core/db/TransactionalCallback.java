package org.zstack.core.db;

public interface TransactionalCallback {
    public static enum Operation {
        PERSIST,
        UPDATE,
        REMOVE,
    }
    
    void suspend(Class<?>...entityClass);
    
    void resume(Class<?>...entityClass);

    void flush(Class<?>...entityClass);

    void beforeCommit(Operation op, boolean readOnly, Class<?>...entityClass);

    void beforeCompletion(Operation op, Class<?>...entityClass);

    void afterCommit(Operation op, Class<?>...entityClass);

    void afterCompletion(Operation op, int status, Class<?>...entityClass);
}
