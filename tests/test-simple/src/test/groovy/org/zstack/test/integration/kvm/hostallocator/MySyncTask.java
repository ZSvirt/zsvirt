package org.zstack.test.integration.kvm.hostallocator;

import org.zstack.core.thread.SyncTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MySyncTask implements SyncTask<Void> {
    private AtomicInteger counter;

    MySyncTask(AtomicInteger cntr) {
        this.counter = cntr;
    }

    @Override
    public String getSyncSignature() {
        return "sync-task-test";
    }

    @Override
    public int getSyncLevel() {
        return 2;
    }

    @Override
    public String getName() {
        return "demo task";
    }

    @Override
    public Void call() throws Exception {
        TimeUnit.SECONDS.sleep(1);
        counter.decrementAndGet();
        return null;
    }
}
