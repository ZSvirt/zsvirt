package org.zstack.core.thread;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.utils.TaskContext;

public class TaskContextCleaner {
    @Autowired
    private ThreadFacade thdf;

    void init() {
        thdf.registerHook(new ThreadAroundHook() {
            @Override
            public void beforeExecute(Thread t, Runnable r) {
                TaskContext.removeTaskContext();
            }

            @Override
            public void afterExecute(Runnable r, Throwable t) {
                TaskContext.removeTaskContext();
            }
        });
    }
}
