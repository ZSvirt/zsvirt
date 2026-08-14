package org.zstack.sdk.managements.common;

import org.zstack.sdk.ErrorCode;

public class ManagementNodeStatusView  {

    public java.lang.String ip;
    public void setIp(java.lang.String ip) {
        this.ip = ip;
    }
    public java.lang.String getIp() {
        return this.ip;
    }

    public java.lang.String gatewayIp;
    public void setGatewayIp(java.lang.String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }
    public java.lang.String getGatewayIp() {
        return this.gatewayIp;
    }

    public boolean ownsVip;
    public void setOwnsVip(boolean ownsVip) {
        this.ownsVip = ownsVip;
    }
    public boolean getOwnsVip() {
        return this.ownsVip;
    }

    public boolean peerReachable;
    public void setPeerReachable(boolean peerReachable) {
        this.peerReachable = peerReachable;
    }
    public boolean getPeerReachable() {
        return this.peerReachable;
    }

    public boolean gatewayReachable;
    public void setGatewayReachable(boolean gatewayReachable) {
        this.gatewayReachable = gatewayReachable;
    }
    public boolean getGatewayReachable() {
        return this.gatewayReachable;
    }

    public boolean vipReachable;
    public void setVipReachable(boolean vipReachable) {
        this.vipReachable = vipReachable;
    }
    public boolean getVipReachable() {
        return this.vipReachable;
    }

    public java.lang.String keepalivedStatus;
    public void setKeepalivedStatus(java.lang.String keepalivedStatus) {
        this.keepalivedStatus = keepalivedStatus;
    }
    public java.lang.String getKeepalivedStatus() {
        return this.keepalivedStatus;
    }

    public java.lang.String haMonitorStatus;
    public void setHaMonitorStatus(java.lang.String haMonitorStatus) {
        this.haMonitorStatus = haMonitorStatus;
    }
    public java.lang.String getHaMonitorStatus() {
        return this.haMonitorStatus;
    }

    public java.lang.String databaseStatus;
    public void setDatabaseStatus(java.lang.String databaseStatus) {
        this.databaseStatus = databaseStatus;
    }
    public java.lang.String getDatabaseStatus() {
        return this.databaseStatus;
    }

    public java.lang.String uiStatus;
    public void setUiStatus(java.lang.String uiStatus) {
        this.uiStatus = uiStatus;
    }
    public java.lang.String getUiStatus() {
        return this.uiStatus;
    }

    public java.lang.String managementsNodeStatus;
    public void setManagementsNodeStatus(java.lang.String managementsNodeStatus) {
        this.managementsNodeStatus = managementsNodeStatus;
    }
    public java.lang.String getManagementsNodeStatus() {
        return this.managementsNodeStatus;
    }

    public boolean slaveIoRunning;
    public void setSlaveIoRunning(boolean slaveIoRunning) {
        this.slaveIoRunning = slaveIoRunning;
    }
    public boolean getSlaveIoRunning() {
        return this.slaveIoRunning;
    }

    public boolean slaveSqlRunning;
    public void setSlaveSqlRunning(boolean slaveSqlRunning) {
        this.slaveSqlRunning = slaveSqlRunning;
    }
    public boolean getSlaveSqlRunning() {
        return this.slaveSqlRunning;
    }

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

}
