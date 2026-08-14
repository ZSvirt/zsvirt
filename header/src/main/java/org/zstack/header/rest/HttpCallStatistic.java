package org.zstack.header.rest;

import java.util.concurrent.TimeUnit;

/**
 */
public class HttpCallStatistic {
    private String url;
    private long totalTime;
    private long count;
    private long averageTime;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getAverageTime() {
        return averageTime;
    }

    public void setAverageTime(long averageTime) {
        this.averageTime = averageTime;
    }

    public void addStatistic(long time) {
        totalTime += time;
        count++;
        averageTime = totalTime / count;
    }

    @Override
    public String toString() {
        return String.format("URL[%s]: total time: %s secs, total count: %s, average time: %s secs",
                url,
                TimeUnit.MILLISECONDS.toSeconds(totalTime),
                count,
                TimeUnit.MILLISECONDS.toSeconds(averageTime));
    }
}
