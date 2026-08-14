package org.zstack.scheduler;

import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.zstack.header.scheduler.SchedulerTriggerVO;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TimeUtils;

import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class SchedulerSimpleTriggerPlanWriter {
    private SimpleTriggerImpl trigger;

    SchedulerSimpleTriggerPlanWriter(SchedulerTriggerVO triggerVO) {
        DebugUtils.Assert(triggerVO.getSchedulerType().equals(SchedulerConstant.SIMPLE_TYPE_STRING), "must be simple trigger.");

        this.trigger = new SimpleTriggerImpl();
        this.trigger.setStartTime(triggerVO.getStartTime());
        this.trigger.setRepeatCount(triggerVO.getRepeatCount() - 1);
        this.trigger.setRepeatInterval(TimeUnit.SECONDS.toMillis(triggerVO.getSchedulerInterval()));
        this.trigger.setEndTime(triggerVO.getStopTime());
    }

    void fillReport(SchedulerExecutionReport report, int magnification) {
        int[] totalFireTimes = new int[report.getRange() + 1];
        Calendar timeNode = report.getStartTime();

        Date beginTime = trigger.getStartTime().before(new Date()) ? new Date() : trigger.getStartTime();
        int startIndex = beginTime.getTime() <= timeNode.getTimeInMillis() ? 0 : totalFireTimes.length;
        for (int i = 0; i < totalFireTimes.length; i++) {
            if (startIndex <= i) {
                totalFireTimes[i] = trigger.computeNumTimesFiredBetween(trigger.getStartTime(), timeNode.getTime());
            } else if (beginTime.getTime() < timeNode.getTimeInMillis()) {
                startIndex = i - 1;
                totalFireTimes[i - 1] = trigger.computeNumTimesFiredBetween(trigger.getStartTime(), beginTime);
                totalFireTimes[i] = trigger.computeNumTimesFiredBetween(trigger.getStartTime(), timeNode.getTime());
            }

            timeNode.add(report.getTimeUnit(), 1);
        }

        int[] results = report.getWaitingRecords();
        for (int i = totalFireTimes.length - 1; i > startIndex; i--) {
            results[i - 1] += (totalFireTimes[i] - totalFireTimes[i - 1]) * magnification;
        }
    }
}
