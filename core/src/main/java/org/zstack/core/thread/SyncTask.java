package org.zstack.core.thread;

public interface SyncTask<T> extends Task<T> {
    String getSyncSignature();
    
    int getSyncLevel();
}
