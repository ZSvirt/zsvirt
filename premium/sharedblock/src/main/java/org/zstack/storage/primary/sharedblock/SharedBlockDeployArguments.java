package org.zstack.storage.primary.sharedblock;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;

public class SharedBlockDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_zsblk")
    private final String packageName = SharedBlockConstants.AGENT_PACKAGE_NAME;
    @SerializedName("free_spcae")
    private Long freeSpace;
    @SerializedName("increment")
    private Long increment;
    @SerializedName("utilization_percent")
    private Long utilizationPercent;
    @SerializedName("maxLockButNotUsedTimes")
    private Long maxLockButNotUsedTimes;
    @SerializedName("scanInterval")
    private Long scanInterval;
    @SerializedName("lvLkProtectionPeriodInSec")
    private Integer lvLkProtectionPeriodInSec;
    @SerializedName("verboseLog")
    private String verboseLog;

    public Long getFreeSpace() {
        return freeSpace;
    }

    public void setFreeSpace(Long freeSpace) {
        this.freeSpace = freeSpace;
    }

    public Long getIncrement() {
        return increment;
    }

    public void setIncrement(Long increment) {
        this.increment = increment;
    }

    public Long getUtilizationPercent() {
        return utilizationPercent;
    }

    public void setUtilizationPercent(Long utilizationPercent) {
        this.utilizationPercent = utilizationPercent;
    }

    public Long getMaxLockButNotUsedTimes() {
        return maxLockButNotUsedTimes;
    }

    public void setMaxLockButNotUsedTimes(Long maxLockButNotUsedTimes) {
        this.maxLockButNotUsedTimes = maxLockButNotUsedTimes;
    }

    public Long getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(Long scanInterval) {
        this.scanInterval = scanInterval;
    }

    public String getVerboseLog() {
        return verboseLog;
    }

    public void setVerboseLog(String verboseLog) {
        this.verboseLog = verboseLog;
    }

    public Integer getLvLkProtectionPeriodInSec() {
        return lvLkProtectionPeriodInSec;
    }

    public void setLvLkProtectionPeriodInSec(Integer lvLkProtectionPeriodInSec) {
        this.lvLkProtectionPeriodInSec = lvLkProtectionPeriodInSec;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }
}
