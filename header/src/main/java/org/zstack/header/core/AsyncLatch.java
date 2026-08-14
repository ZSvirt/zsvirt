package org.zstack.header.core;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by frank on 8/5/2015.
 */
public class AsyncLatch extends AbstractCompletion {
    private int count;
    private AtomicInteger atomicInteger = new AtomicInteger(0);
    private NoErrorCompletion completion;

    public AsyncLatch(int count, NoErrorCompletion completion) {
        super(completion);
        this.count = count;
        this.completion = completion;
    }

    public void ack() {
        if (atomicInteger.incrementAndGet() == count) {
            completion.done();
        }
    }
}
