package org.zstack.sdk;



public class VmSpendingDetails  {

    public long startTime;
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    public long getStartTime() {
        return this.startTime;
    }

    public long endTime;
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    public long getEndTime() {
        return this.endTime;
    }

    public double spending;
    public void setSpending(double spending) {
        this.spending = spending;
    }
    public double getSpending() {
        return this.spending;
    }

}
