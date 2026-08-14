package org.zstack.sdk;

import org.zstack.sdk.HypervisorVersionState;

public class KvmHypervisorInfoInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String hypervisor;
    public void setHypervisor(java.lang.String hypervisor) {
        this.hypervisor = hypervisor;
    }
    public java.lang.String getHypervisor() {
        return this.hypervisor;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

    public HypervisorVersionState matchState;
    public void setMatchState(HypervisorVersionState matchState) {
        this.matchState = matchState;
    }
    public HypervisorVersionState getMatchState() {
        return this.matchState;
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
