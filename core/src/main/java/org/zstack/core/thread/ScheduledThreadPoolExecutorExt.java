package org.zstack.core.thread;

import org.apache.logging.log4j.ThreadContext;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;


public class ScheduledThreadPoolExecutorExt extends ScheduledThreadPoolExecutor {
    private static final CLogger _logger =CLoggerImpl.getLogger(ScheduledThreadPoolExecutorExt.class);
    
    List<ThreadAroundHook> _hooks = new ArrayList<ThreadAroundHook>(8);

    public ScheduledThreadPoolExecutorExt(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
        this.setMaximumPoolSize(corePoolSize);
    }
    
    public void registerHook(ThreadAroundHook hook) {
        synchronized (_hooks) {
            _hooks.add(hook);
        }
    }
    
    public void unregisterHook(ThreadAroundHook hook) {
        synchronized (_hooks) {
            _hooks.remove(hook);
        }
    }
    
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        ThreadContext.clearMap();
        ThreadContext.clearStack();

        ThreadAroundHook debugHook = null;
        List<ThreadAroundHook> tmpHooks;       
        synchronized (_hooks) {
            tmpHooks = new ArrayList<ThreadAroundHook>(_hooks);
        }
        
        for (ThreadAroundHook hook : tmpHooks) {
            debugHook = hook;
            try {
                hook.beforeExecute(t, r);
            } catch (Exception e) {
                _logger.warn("Unhandled exception happened during executing ThreadAroundHook: " + debugHook.getClass().getCanonicalName(), e);
            }
        }
    }
    
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        ThreadContext.clearMap();
        ThreadContext.clearStack();

        ThreadAroundHook debugHook = null;
        List<ThreadAroundHook> tmpHooks;
        synchronized (_hooks) {
            tmpHooks = new ArrayList<ThreadAroundHook>(_hooks);
        }
        
        for (ThreadAroundHook hook : tmpHooks) {
            debugHook = hook;
            try {
                hook.afterExecute(r, t);
            } catch (Exception e) {
                _logger.warn("Unhandled exception happened during executing ThreadAroundHook: " + debugHook.getClass().getCanonicalName(), e);
            }
        }
    }
}
