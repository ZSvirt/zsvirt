package org.zstack.scheduler;

import org.quartz.TriggerUtils;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DateCountCache;
import org.zstack.utils.TimeUtils;

import java.text.ParseException;
import java.util.*;

/**
 * Created by MaJin on 2020/3/30.
 */
public class SchedulerCronExecutionPlan {
    private DateCountCache plan;
    private CronTriggerImpl trigger, originTrigger;

    SchedulerCronExecutionPlan(String cron) {
        this.trigger = new CronTriggerImpl();
        try {
            trigger.setCronExpression(cron);
        } catch (ParseException e) {
            throw new CloudRuntimeException(e);
        }

        Calendar time = Calendar.getInstance();
        int nowYear = time.get(Calendar.YEAR);
        this.plan = new DateCountCache(nowYear, nowYear);
        this.originTrigger = (CronTriggerImpl) trigger.clone();
    }

    synchronized void fillReport(SchedulerExecutionReport report, Date triggerStartTime, int magnification) {
        refreshUntil(report.getEndTime());

        Calendar reportStartTime = report.getStartTime();
        Calendar calculateStartTime = Calendar.getInstance();
        calculateStartTime.setTimeInMillis(triggerStartTime == null || triggerStartTime.getTime() < System.currentTimeMillis()
                ? System.currentTimeMillis() : triggerStartTime.getTime());

        boolean started = calculateStartTime.before(reportStartTime);
        for (int i = 0; i < report.getRange(); i++) {
            if (started) {
                report.getWaitingRecords()[i] += getCountOnWholeTimeUnit(reportStartTime, report.getTimeUnit()) * magnification;
            } else if (TimeUtils.equalApproximately(reportStartTime, calculateStartTime, report.getTimeUnit())) {
                started = true;
                report.getWaitingRecords()[i] += getCountOnPartialTimeUnit(calculateStartTime, report.getTimeUnit()) * magnification;
            }

            reportStartTime.add(report.getTimeUnit(), 1);
        }
    }

    private int getCountOnWholeTimeUnit(Calendar time, int timeUnit) {
        if (timeUnit == Calendar.HOUR_OF_DAY) {
            return recalculate(TimeUtils.roundOff(time, timeUnit), TimeUtils.roundUp(time, timeUnit));
        }

        return plan.getCount(time, timeUnit);
    }

    private int getCountOnPartialTimeUnit(Calendar startTime, int timeUnit) {
        if (timeUnit == Calendar.HOUR_OF_DAY) {
            return recalculate(startTime, TimeUtils.roundUp(startTime, timeUnit));
        }

        Calendar nextDay = TimeUtils.roundUp(startTime, Calendar.DATE);
        int startDayCount = recalculate(startTime, nextDay);

        if (timeUnit == Calendar.DATE) {
            return startDayCount;
        } else {
            int sum = plan.sum(nextDay, TimeUtils.roundUp(startTime, timeUnit));
            sum += startDayCount;
            return sum;
        }
    }

    private void refreshUntil(Calendar endTime) {
        if (trigger.getNextFireTime() == null) {
            trigger.setStartTime(TimeUtils.roundOff(System.currentTimeMillis(), Calendar.DATE).getTime());
            trigger.computeFirstFireTime(null);
        }

        plan.expandIfNeed(null, endTime.get(Calendar.YEAR));

        Date nextTime;
        while ((nextTime = trigger.getNextFireTime()).before(endTime.getTime())) {
            plan.addCountUnsafe(nextTime.getYear() + 1900, nextTime.getMonth(), nextTime.getDate());
            trigger.triggered(null);
        }
    }

    /**
     * @param from include
     * @param to exclude
     * @return fire times
     */
    private int recalculate(Calendar from, Calendar to) {
        return TriggerUtils.computeFireTimesBetween(originTrigger, null, from.getTime(), new Date(to.getTimeInMillis() - 1)).size();
    }
}
