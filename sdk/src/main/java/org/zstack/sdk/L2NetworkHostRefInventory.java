package org.zstack.sdk;



public class L2NetworkHostRefInventory  {

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String l2NetworkUuid;
    public void setL2NetworkUuid(java.lang.String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }
    public java.lang.String getL2NetworkUuid() {
        return this.l2NetworkUuid;
    }

    public java.lang.String l2ProviderType;
    public void setL2ProviderType(java.lang.String l2ProviderType) {
        this.l2ProviderType = l2ProviderType;
    }
    public java.lang.String getL2ProviderType() {
        return this.l2ProviderType;
    }

    public java.lang.String bridgeName;
    public void setBridgeName(java.lang.String bridgeName) {
        this.bridgeName = bridgeName;
    }
    public java.lang.String getBridgeName() {
        return this.bridgeName;
    }

    public boolean skipDeletion;
    public void setSkipDeletion(boolean skipDeletion) {
        this.skipDeletion = skipDeletion;
    }
    public boolean getSkipDeletion() {
        return this.skipDeletion;
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
