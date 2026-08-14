package org.zstack.sdk;

import org.zstack.sdk.VmPriorityLevel;

public class VmPriorityConfigInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
    }

    public VmPriorityLevel level;
    public void setLevel(VmPriorityLevel level) {
        this.level = level;
    }
    public VmPriorityLevel getLevel() {
        return this.level;
    }

    public int cpuShares;
    public void setCpuShares(int cpuShares) {
        this.cpuShares = cpuShares;
    }
    public int getCpuShares() {
        return this.cpuShares;
    }

    public int oomScoreAdj;
    public void setOomScoreAdj(int oomScoreAdj) {
        this.oomScoreAdj = oomScoreAdj;
    }
    public int getOomScoreAdj() {
        return this.oomScoreAdj;
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
