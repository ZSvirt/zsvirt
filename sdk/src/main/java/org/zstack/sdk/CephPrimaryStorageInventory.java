package org.zstack.sdk;



public class CephPrimaryStorageInventory extends org.zstack.sdk.PrimaryStorageInventory {

    public java.util.List mons;
    public void setMons(java.util.List mons) {
        this.mons = mons;
    }
    public java.util.List getMons() {
        return this.mons;
    }

    public java.util.List pools;
    public void setPools(java.util.List pools) {
        this.pools = pools;
    }
    public java.util.List getPools() {
        return this.pools;
    }

    public java.lang.String fsid;
    public void setFsid(java.lang.String fsid) {
        this.fsid = fsid;
    }
    public java.lang.String getFsid() {
        return this.fsid;
    }

}
