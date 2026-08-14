package org.zstack.sdk;

import org.zstack.sdk.RunningState;

public class PhysicalDriveSmartSelfTestHistoryInventory  {

    public java.lang.Long id;
    public void setId(java.lang.Long id) {
        this.id = id;
    }
    public java.lang.Long getId() {
        return this.id;
    }

    public java.lang.String raidPhysicalDriveUuid;
    public void setRaidPhysicalDriveUuid(java.lang.String raidPhysicalDriveUuid) {
        this.raidPhysicalDriveUuid = raidPhysicalDriveUuid;
    }
    public java.lang.String getRaidPhysicalDriveUuid() {
        return this.raidPhysicalDriveUuid;
    }

    public RunningState runningState;
    public void setRunningState(RunningState runningState) {
        this.runningState = runningState;
    }
    public RunningState getRunningState() {
        return this.runningState;
    }

    public java.lang.String testResult;
    public void setTestResult(java.lang.String testResult) {
        this.testResult = testResult;
    }
    public java.lang.String getTestResult() {
        return this.testResult;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
