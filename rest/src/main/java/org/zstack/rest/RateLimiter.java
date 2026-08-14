package org.zstack.rest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    private final LoadingCache<String, TokenBucket> requestCache;
    private final int maxRequestsPerMinute;

    public RateLimiter(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.requestCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<String, TokenBucket>() {
                    @Override
                    public TokenBucket load(String key) {
                        return new TokenBucket(maxRequestsPerMinute);
                    }
                });
    }

    public boolean isRateLimitExceeded(String clientIp) {
        try {
            TokenBucket bucket = requestCache.get(clientIp);
            return !bucket.tryConsume();
        } catch (ExecutionException e) {
            // 处理异常
            return false;
        }
    }

    private static class TokenBucket {
        private final Queue<Long> timestamps = new ConcurrentLinkedQueue<>();
        private final int capacity;

        public TokenBucket(int capacity) {
            this.capacity = capacity;
        }

        public synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            long boundary = now - TimeUnit.MINUTES.toMillis(1);

            // remove time stamps over boundary
            while (!timestamps.isEmpty() && timestamps.peek() <= boundary) {
                timestamps.poll();
            }

            // check requests limitation
            if (timestamps.size() >= capacity) {
                return false;
            }

            // add latest request timestamp
            timestamps.offer(now);
            return true;
        }
    }
}
