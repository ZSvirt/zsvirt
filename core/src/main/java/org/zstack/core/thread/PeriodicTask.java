package org.zstack.core.thread;

import org.zstack.header.HasThreadContext;

import java.util.concurrent.TimeUnit;

public interface PeriodicTask extends Runnable, HasThreadContext {
    TimeUnit getTimeUnit();
    
    long getInterval();
    
    String getName();
}
