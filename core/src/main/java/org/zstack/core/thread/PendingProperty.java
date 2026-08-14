package org.zstack.core.thread;

import java.util.concurrent.Callable;

/**
 * Created by mingjian.deng on 2019/10/15.
 */
public class PendingProperty {
    private int maxPendingSize = -1;
    private Callable<Boolean> callable = new Callable<Boolean>() {
        @Override
        public Boolean call() {
            return false;
        }
    };

    public int getMaxPendingSize() {
        return maxPendingSize;
    }

    public void setMaxPendingSize(int maxPendingSize) {
        this.maxPendingSize = maxPendingSize;
    }

    public Callable<Boolean> getCallable() {
        return callable;
    }

    public void setCallable(Callable<Boolean> callable) {
        this.callable = callable;
    }
}
