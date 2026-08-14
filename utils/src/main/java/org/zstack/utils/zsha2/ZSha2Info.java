package org.zstack.utils.zsha2;

/**
 * Created by mingjian.deng on 2020/4/2.
 */
public class ZSha2Info {
    private String nodeip;
    private String peerip;
    private String dbvip;
    private String nic;
    private boolean isMaster;

    public String getNodeip() {
        return nodeip;
    }

    public void setNodeip(String nodeip) {
        this.nodeip = nodeip;
    }

    public String getPeerip() {
        return peerip;
    }

    public void setPeerip(String peerip) {
        this.peerip = peerip;
    }

    public String getDbvip() {
        return dbvip;
    }

    public void setDbvip(String dbvip) {
        this.dbvip = dbvip;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public void setMaster(boolean master) {
        isMaster = master;
    }
}
