package org.zstack.core.cloudbus;

import org.zstack.header.core.AbstractCompositeType;
import org.zstack.header.exception.CloudRuntimeException;

import javax.management.MXBean;
import javax.management.openmbean.*;

/**
 */
@MXBean
public class MessageStatistic extends AbstractCompositeType {
    private String messageClassName;
    private long totalTime;
    private long averageTime;
    private long count;
    private long maxTime;
    private long minTime;

    static String[] fieldNames = new String[] {"messageClassName", "totalTime", "averageTime", "count", "maxTime", "minTime"};

    public String getMessageClassName() {
        return messageClassName;
    }

    public void setMessageClassName(String messageClassName) {
        this.messageClassName = messageClassName;
    }

    public long getAverageTime() {
        return averageTime;
    }

    public void setAverageTime(long averageTime) {
        this.averageTime = averageTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    public long getMinTime() {
        return minTime;
    }

    public void setMinTime(long minTime) {
        this.minTime = minTime;
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

    public void count(long cost) {
        if (Long.MAX_VALUE - cost < totalTime) {
            totalTime = cost;
            count = 1;
        } else {
            totalTime += cost;
            count ++;
        }

        averageTime = totalTime / count;
        maxTime = Math.max(cost, maxTime);
        minTime = Math.min(cost, minTime);
    }

    @Override
    protected String[] getFieldNames() {
        return fieldNames;
    }

    @Override
    public CompositeType getCompositeType() {
        try {
            return new CompositeType(
                    "MessageStatistic",
                    "message statistic struct",
                    fieldNames,
                    fieldNames,
                    new OpenType[] {SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG}
            );
        } catch (OpenDataException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
