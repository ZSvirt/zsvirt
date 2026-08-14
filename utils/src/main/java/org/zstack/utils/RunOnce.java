package org.zstack.utils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xing5 on 2016/8/2.
 */
public class RunOnce {
    private AtomicBoolean hasRun = new AtomicBoolean(false);

    public void run(Runnable runnable) {
        if (!hasRun.compareAndSet(false, true)) {
            return;
        }

        runnable.run();
    }
}
