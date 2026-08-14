package org.zstack.scheduler;

import org.zstack.utils.TimeUtils;

import java.util.*;

/**
 * Created by MaJin on 2020/3/30.
 */
public class SchedulerExecutionReport {
    private final Calendar startTime;
    private final int timeUnit;    //valid values: Calendar.HOUR_OF_DAY, Calendar.DATE, Calendar.MONTH;
    private final int range;
    private final long nowTimeInMiLls;

    private final int[] successRecords;
    private final int[] failureRecords;
    private final int[] partialSuccessRecords;
    private final int[] waitingRecords;

    SchedulerExecutionReport(long startTimeInMills, int timeUnit, int range) {
        this.startTime = TimeUtils.roundOff(startTimeInMills, timeUnit);
        this.timeUnit = timeUnit;
        this.range = range;
        this.nowTimeInMiLls = System.currentTimeMillis();

        successRecords = new int[range];
        failureRecords = new int[range];
        partialSuccessRecords = new int[range];
        waitingRecords = new int[range];
    }

    public Calendar getStartTime() {
        return (Calendar) startTime.clone();
    }

    int getTimeUnit() {
        return timeUnit;
    }

    int getRange() {
        return range;
    }

    public int[] getSuccessRecords() {
        return successRecords;
    }

    public int[] getFailureRecords() {
        return failureRecords;
    }

    public int[] getPartialSuccessRecords() {
        return partialSuccessRecords;
    }

    public int[] getWaitingRecords() {
        return waitingRecords;
    }

    public long getNowTimeInMiLls() {
        return nowTimeInMiLls;
    }

    public Calendar getEndTime() {
        Calendar endTime = (Calendar) startTime.clone();
        endTime.add(timeUnit, range);
        return endTime;
    }
}
