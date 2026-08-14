package org.zstack.core.thread;

import org.zstack.header.HasThreadContext;
import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;

import java.util.concurrent.TimeUnit;

public abstract class CancelablePeriodicTask extends AbstractCompletion implements HasThreadContext {
    protected CancelablePeriodicTask(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public CancelablePeriodicTask() {
        super(null);
    }

    public abstract boolean run();
	
    public abstract TimeUnit getTimeUnit();
    
    public abstract long getInterval();
    
    public abstract String getName();
}
