package org.zstack.billing;

/**
 * Created by xing5 on 2016/6/7.
 */
public abstract class UsageSample {
    protected String accountUuid;
    protected double usage;
    protected long startTime;
    protected long endTime;
    protected double cost;
    protected String resourcePriceUserConfig;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public double getUsage() {
        return usage;
    }

    public void setUsage(double usage) {
        this.usage = usage;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getResourcePriceUserConfig() {
        return resourcePriceUserConfig;
    }

    public void setResourcePriceUserConfig(String resourcePriceUserConfig) {
        this.resourcePriceUserConfig = resourcePriceUserConfig;
    }
}
