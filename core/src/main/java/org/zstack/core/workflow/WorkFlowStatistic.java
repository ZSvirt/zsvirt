package org.zstack.core.workflow;

import java.util.concurrent.TimeUnit;

/**
 */
public class WorkFlowStatistic {
    private volatile long totalTime;
    private volatile long count;
    private volatile long averageTime;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        count ++;
        averageTime = totalTime / count;
    }

    @Override
    public String toString() {
        return String.format("Flow[%s]: total time: %s secs, total count: %s, average time: %s secs",
                name,
                TimeUnit.MILLISECONDS.toSeconds(totalTime),
                count,
                TimeUnit.MILLISECONDS.toSeconds(averageTime));
    }
}
