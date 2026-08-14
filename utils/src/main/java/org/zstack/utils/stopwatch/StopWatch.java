package org.zstack.utils.stopwatch;

import java.util.concurrent.TimeUnit;

public interface StopWatch {
    void start();
    
    void stop();
    
    long getLapse();
    
    long getLapse(TimeUnit unit);
}
