package org.zstack.utils.zsha2;

import com.google.gson.annotations.SerializedName;

/**
 * @author hanyu.liang
 * @date 2023/11/6 17:02
 */
public class ZSha2StatusJsonInfo {
    private boolean ownsVip;
    private boolean peerReachable;
    private boolean gwReachable;
    private boolean vipReachable;
    private String dbStatus;
    private String mnStatus;
    private String timeToSyncDB;
    private boolean slaveIoRunning;
    @SerializedName("slaveSqlRuning") // zsha2 typo
    private boolean slaveSqlRunning;

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

    public boolean isGwReachable() {
        return gwReachable;
    }

    public void setGwReachable(boolean gwReachable) {
        this.gwReachable = gwReachable;
    }

    public boolean isVipReachable() {
        return vipReachable;
    }

    public void setVipReachable(boolean vipReachable) {
        this.vipReachable = vipReachable;
    }

    public String getDbStatus() {
        return dbStatus;
    }

    public void setDbStatus(String dbStatus) {
        this.dbStatus = dbStatus;
    }

    public String getMnStatus() {
        return mnStatus;
    }

    public void setMnStatus(String mnStatus) {
        this.mnStatus = mnStatus;
    }

    public String getTimeToSyncDB() {
        return timeToSyncDB;
    }

    public void setTimeToSyncDB(String timeToSyncDB) {
        this.timeToSyncDB = timeToSyncDB;
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
}
