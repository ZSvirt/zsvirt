package org.zstack.billing;

import java.util.Date;

/**
 * @author: kefeng.wang
 * @date: 2019-01-14
 **/
public class ResourceSpendingPeriod implements Comparable<ResourceSpendingPeriod> {
    private Date startTime;
    private Date endTime;
    private double spending;

    public ResourceSpendingPeriod() {
    }

    public ResourceSpendingPeriod(Date startTime, Date endTime, double spending) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.spending = spending;
    }

    public ResourceSpendingPeriod(long startTime, long endTime, double spending) {
        this.startTime = new Date(startTime);
        this.endTime = new Date(endTime);
        this.spending = spending;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public double getSpending() {
        return spending;
    }

    public void setSpending(double spending) {
        this.spending = spending;
    }

    @Override
    public int compareTo(ResourceSpendingPeriod other) {
        return this.getStartTime().compareTo(other.getStartTime());
    }

    public void setStartTimeMin(Date startTime) {
        if (this.startTime == null || this.startTime.compareTo(startTime) > 0) {
            this.startTime = startTime;
        }
    }

    public void setEndTimeMax(Date endTime) {
        if (this.endTime == null || this.endTime.compareTo(endTime) < 0) {
            this.endTime = endTime;
        }
    }

    public void setSpendingSum(double spending) {
        this.spending += spending;
    }

    public long getPeriodMillis() {
        return endTime.getTime() - startTime.getTime() + 1;
    }
}
