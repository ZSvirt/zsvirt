package org.zstack.sdk;



public class DataVolumeSpendingInventory  {

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

    public long volumeSize;
    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }
    public long getVolumeSize() {
        return this.volumeSize;
    }

}
