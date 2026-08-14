package org.zstack.sdk.tpm.entity;



public class TpmCapabilityView  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
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

    public java.util.List fileRefs;
    public void setFileRefs(java.util.List fileRefs) {
        this.fileRefs = fileRefs;
    }
    public java.util.List getFileRefs() {
        return this.fileRefs;
    }

    public java.lang.String edkVersion;
    public void setEdkVersion(java.lang.String edkVersion) {
        this.edkVersion = edkVersion;
    }
    public java.lang.String getEdkVersion() {
        return this.edkVersion;
    }

    public java.lang.String swtpmVersion;
    public void setSwtpmVersion(java.lang.String swtpmVersion) {
        this.swtpmVersion = swtpmVersion;
    }
    public java.lang.String getSwtpmVersion() {
        return this.swtpmVersion;
    }

    public boolean resetTpmAfterVmCloneConfig;
    public void setResetTpmAfterVmCloneConfig(boolean resetTpmAfterVmCloneConfig) {
        this.resetTpmAfterVmCloneConfig = resetTpmAfterVmCloneConfig;
    }
    public boolean getResetTpmAfterVmCloneConfig() {
        return this.resetTpmAfterVmCloneConfig;
    }

}
