package org.zstack.managements.entity.common;

import org.zstack.header.errorcode.ErrorCode;

public class ManagementNodeStatusView {
    private String ip;
    private String gatewayIp;
    private boolean ownsVip;
    private boolean peerReachable;
    private boolean gatewayReachable;
    private boolean vipReachable;
    private String keepalivedStatus;
    private String haMonitorStatus;
    private String databaseStatus;
    private String uiStatus;
    private String managementsNodeStatus;
    private boolean slaveIoRunning;
    private boolean slaveSqlRunning;
    private ErrorCode error;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public boolean isOwnsVip() {
        return ownsVip;
    }

    public void setOwnsVip(boolean ownsVip) {
        this.ownsVip = ownsVip;
    }

    public boolean isPeerReachable() {
        return peerReachable;
    }

    public void setPeerReachable(boolean peerReachable) {
        this.peerReachable = peerReachable;
    }

    public boolean isGatewayReachable() {
        return gatewayReachable;
    }

    public void setGatewayReachable(boolean gatewayReachable) {
        this.gatewayReachable = gatewayReachable;
    }

    public boolean isVipReachable() {
        return vipReachable;
    }

    public void setVipReachable(boolean vipReachable) {
        this.vipReachable = vipReachable;
    }

    public String getKeepalivedStatus() {
        return keepalivedStatus;
    }

    public void setKeepalivedStatus(String keepalivedStatus) {
        this.keepalivedStatus = keepalivedStatus;
    }

    public String getHaMonitorStatus() {
        return haMonitorStatus;
    }

    public void setHaMonitorStatus(String haMonitorStatus) {
        this.haMonitorStatus = haMonitorStatus;
    }

    public String getDatabaseStatus() {
        return databaseStatus;
    }

    public void setDatabaseStatus(String databaseStatus) {
        this.databaseStatus = databaseStatus;
    }

    public String getUiStatus() {
        return uiStatus;
    }

    public void setUiStatus(String uiStatus) {
        this.uiStatus = uiStatus;
    }

    public String getManagementsNodeStatus() {
        return managementsNodeStatus;
    }

    public void setManagementsNodeStatus(String managementsNodeStatus) {
        this.managementsNodeStatus = managementsNodeStatus;
    }

    public boolean isSlaveIoRunning() {
        return slaveIoRunning;
    }

    public void setSlaveIoRunning(boolean slaveIoRunning) {
        this.slaveIoRunning = slaveIoRunning;
    }

    public boolean isSlaveSqlRunning() {
        return slaveSqlRunning;
    }

    public void setSlaveSqlRunning(boolean slaveSqlRunning) {
        this.slaveSqlRunning = slaveSqlRunning;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }

    public static ManagementNodeStatusView __example1__() {
        ManagementNodeStatusView view = new ManagementNodeStatusView();
        view.setIp("172.26.21.212");
        view.setGatewayIp("172.26.0.1");
        view.setOwnsVip(true);
        view.setPeerReachable(true);
        view.setGatewayReachable(true);
        view.setVipReachable(true);
        view.setKeepalivedStatus("active");
        view.setHaMonitorStatus("active");
        view.setDatabaseStatus("mysqld is alive");
        view.setUiStatus("running");
        view.setManagementsNodeStatus("running");
        view.setSlaveIoRunning(true);
        view.setSlaveSqlRunning(true);
        return view;
    }

    public static ManagementNodeStatusView __example2__() {
        ManagementNodeStatusView view = new ManagementNodeStatusView();
        view.setIp("172.26.30.106");
        view.setGatewayIp("172.26.0.1");
        view.setOwnsVip(false);
        view.setPeerReachable(true);
        view.setGatewayReachable(true);
        view.setVipReachable(true);
        view.setKeepalivedStatus("active");
        view.setHaMonitorStatus("active");
        view.setDatabaseStatus("mysqld is alive");
        view.setUiStatus("running");
        view.setManagementsNodeStatus("running");
        view.setSlaveIoRunning(true);
        view.setSlaveSqlRunning(true);
        return view;
    }
}
