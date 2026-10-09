package org.zstack.test.integration.core;

import org.zstack.core.thread.PeriodicTask;

import java.util.concurrent.TimeUnit;

public class TestPeriodicTask implements PeriodicTask {
    private final String name;
    private final Runnable body;

    public TestPeriodicTask(String name, Runnable body) {
        this.name = name;
        this.body = body;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    public long getInterval() {
        return 200L;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void run() {
        body.run();
    }
}
