package org.zstack.sdk;



public class ManagementNodeInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String hostName;
    public void setHostName(java.lang.String hostName) {
        this.hostName = hostName;
    }
    public java.lang.String getHostName() {
        return this.hostName;
    }

    public java.sql.Timestamp joinDate;
    public void setJoinDate(java.sql.Timestamp joinDate) {
        this.joinDate = joinDate;
    }
    public java.sql.Timestamp getJoinDate() {
        return this.joinDate;
    }

    public java.sql.Timestamp heartBeat;
    public void setHeartBeat(java.sql.Timestamp heartBeat) {
        this.heartBeat = heartBeat;
    }
    public java.sql.Timestamp getHeartBeat() {
        return this.heartBeat;
    }

}
