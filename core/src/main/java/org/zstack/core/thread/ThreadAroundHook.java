package org.zstack.core.thread;

public interface ThreadAroundHook {
    void beforeExecute(Thread t, Runnable r);
    
    void afterExecute(Runnable r, Throwable t);
}
