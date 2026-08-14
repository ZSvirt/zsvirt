package org.zstack.sdk;



public class IscsiTargetInventory  {

    public java.lang.String iscsiServerUuid;
    public void setIscsiServerUuid(java.lang.String iscsiServerUuid) {
        this.iscsiServerUuid = iscsiServerUuid;
    }
    public java.lang.String getIscsiServerUuid() {
        return this.iscsiServerUuid;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String iqn;
    public void setIqn(java.lang.String iqn) {
        this.iqn = iqn;
    }
    public java.lang.String getIqn() {
        return this.iqn;
    }

    public java.util.List iscsiLuns;
    public void setIscsiLuns(java.util.List iscsiLuns) {
        this.iscsiLuns = iscsiLuns;
    }
    public java.util.List getIscsiLuns() {
        return this.iscsiLuns;
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
