package org.zstack.core.singleflight;

import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.*;

import static org.zstack.core.Platform.operr;

/**
 * Created by Wenhao.Zhang on 20/11/10
 */
public abstract class AbstractSingleFlight<K, V> {
    protected final Map<K, SingleFlightContext> calls = new HashMap<>();

    protected Collection<ReturnValueCompletion<V>> dequeAll(K key) {
        synchronized (this) {
            SingleFlightContext context = calls.remove(key);
            return context == null ? null : context.getQueue();
        }
    }

    /**
     * Add completion to queue. The first one of queue executes, and others wait for it.
     * @return
     *   If it is the first consumer to join the queue for the specific key, return true, otherwise false.
     */
    protected synchronized boolean join(K key, ReturnValueCompletion<V> completion) {
        SingleFlightContext context = calls.computeIfAbsent(key, this::buildContext);
        boolean first = context.getQueue().isEmpty();
        context.add(completion);
        return first;
    }

    public synchronized int count(K key) {
        final SingleFlightContext context = calls.getOrDefault(key, INVALID);
        return context.count();
    }

    @AsyncThread
    protected void success(ReturnValueCompletion<V> consumer, final V v) {
        consumer.success(v);
    }
    
    @AsyncThread
    protected void fail(ReturnValueCompletion<V> consumer, ErrorCode errorCode) {
        consumer.fail(errorCode);
    }
    
    @AsyncThread
    protected void fail(ReturnValueCompletion<V> consumer, Exception ex) {
        consumer.fail(operr(ex.getMessage()));
    }
    
    protected void notifyResult(Collection<ReturnValueCompletion<V>> consumers, final V v) {
        consumers.forEach(c -> success(c, v));
    }
    
    protected void notifyFailure(Collection<ReturnValueCompletion<V>> consumers, ErrorCode errorCode) {
        consumers.forEach(c -> fail(c, errorCode));
    }
    
    protected void notifyFailure(Collection<ReturnValueCompletion<V>> consumers, Exception ex) {
        consumers.forEach(c -> fail(c, ex));
    }

    protected synchronized SingleFlightContext getContext(K key) {
        return this.calls.get(key);
    }

    protected SingleFlightContext buildContext(K key) {
        return new SingleFlightContext();
    }

    protected class SingleFlightContext {
        private final List<ReturnValueCompletion<V>> queue = new ArrayList<>();
        private ReturnValueCompletion<V> first;

        public ReturnValueCompletion<V> getFirst() {
            return first;
        }

        public List<ReturnValueCompletion<V>> getQueue() {
            return queue;
        }
        public synchronized void add(ReturnValueCompletion<V> completion) {
            if (queue.isEmpty()) {
                this.first = completion;
            }
            queue.add(completion);
        }

        /**
         * @return
         *   queue size include the execution thread, at lease one
         */
        public int count() {
            return getQueue().size();
        }
        public int pendingCount() {
            return count() - 1;
        }
    }

    private class InvalidSingleFlightContext extends SingleFlightContext {
        @Override
        public List<ReturnValueCompletion<V>> getQueue() {
            return Collections.emptyList();
        }
    }
    protected final InvalidSingleFlightContext INVALID = new InvalidSingleFlightContext();
}
