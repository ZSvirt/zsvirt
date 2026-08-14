package org.zstack.billing;

/**
 * Created by kefeng.wang on 2019-01-04
 */
public class ResourceSpending {
    private String resourceType;
    private String resourceUuid;
    private String resourceName;
    private double spending;
    private Long startTime;
    private Long endTime;

    public ResourceSpending() {
    }

    public ResourceSpending(String resourceType, String resourceUuid, String resourceName,
                            double spending, Long startTime, Long endTime) {
        this.resourceType = resourceType;
        this.resourceUuid = resourceUuid;
        this.resourceName = resourceName;
        this.spending = spending;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public double getSpending() {
        return spending;
    }

    public void setSpending(double spending) {
        this.spending = spending;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}
