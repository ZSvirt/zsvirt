package org.zstack.header.storage.primary;

import org.zstack.header.rest.SDK;

import java.util.List;

@SDK
public class UsageReport {
    List<Long> usedPhysicalCapacitiesForecast;
    List<Long> usedPhysicalCapacitiesHistory;
    List<Long> totalPhysicalCapacitiesHistory;
    Long startTime;
    Long interval;

    public UsageReport() {
    }

    public List<Long> getUsedPhysicalCapacitiesForecast() {
        return usedPhysicalCapacitiesForecast;
    }

    public void setUsedPhysicalCapacitiesForecast(List<Long> usedPhysicalCapacitiesForecast) {
        this.usedPhysicalCapacitiesForecast = usedPhysicalCapacitiesForecast;
    }

    public List<Long> getUsedPhysicalCapacitiesHistory() {
        return usedPhysicalCapacitiesHistory;
    }

    public void setUsedPhysicalCapacitiesHistory(List<Long> usedPhysicalCapacitiesHistory) {
        this.usedPhysicalCapacitiesHistory = usedPhysicalCapacitiesHistory;
    }

    public List<Long> getTotalPhysicalCapacitiesHistory() {
        return totalPhysicalCapacitiesHistory;
    }

    public void setTotalPhysicalCapacitiesHistory(List<Long> totalPhysicalCapacitiesHistory) {
        this.totalPhysicalCapacitiesHistory = totalPhysicalCapacitiesHistory;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }
}
