package org.zstack.utils.stopwatch;

import java.util.concurrent.TimeUnit;

public class StopWatchImpl implements StopWatch {
    private long start = 0;
    private long end = 0;
    private boolean isRunning = false;
    
    @Override
    public void start() {
        start = System.currentTimeMillis();
        isRunning = true;
    }

    @Override
    public void stop() {
        end = System.currentTimeMillis();
        isRunning = false;
    }

    @Override
    public long getLapse() {
        if (!isRunning) {
            return end - start;
        } else {
            return System.currentTimeMillis() - start;
        }
    }

    @Override
    public long getLapse(TimeUnit unit) {
        return unit.convert(getLapse(), TimeUnit.MILLISECONDS);
    }

}
