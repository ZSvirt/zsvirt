package org.zstack.sdk;



public class VmMetadataScanEntry  {

    public java.lang.String vmUuid;
    public void setVmUuid(java.lang.String vmUuid) {
        this.vmUuid = vmUuid;
    }
    public java.lang.String getVmUuid() {
        return this.vmUuid;
    }

    public java.lang.String vmName;
    public void setVmName(java.lang.String vmName) {
        this.vmName = vmName;
    }
    public java.lang.String getVmName() {
        return this.vmName;
    }

    public java.lang.String vmCategory;
    public void setVmCategory(java.lang.String vmCategory) {
        this.vmCategory = vmCategory;
    }
    public java.lang.String getVmCategory() {
        return this.vmCategory;
    }

    public java.lang.String architecture;
    public void setArchitecture(java.lang.String architecture) {
        this.architecture = architecture;
    }
    public java.lang.String getArchitecture() {
        return this.architecture;
    }

    public java.lang.String schemaVersion;
    public void setSchemaVersion(java.lang.String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
    public java.lang.String getSchemaVersion() {
        return this.schemaVersion;
    }

    public java.lang.String metadataPath;
    public void setMetadataPath(java.lang.String metadataPath) {
        this.metadataPath = metadataPath;
    }
    public java.lang.String getMetadataPath() {
        return this.metadataPath;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public long sizeBytes;
    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    public long getSizeBytes() {
        return this.sizeBytes;
    }

    public long lastUpdateTime;
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    public long getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    public boolean incomplete;
    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }
    public boolean getIncomplete() {
        return this.incomplete;
    }

    public boolean regenerateUuidRequired;
    public void setRegenerateUuidRequired(boolean regenerateUuidRequired) {
        this.regenerateUuidRequired = regenerateUuidRequired;
    }
    public boolean getRegenerateUuidRequired() {
        return this.regenerateUuidRequired;
    }

}
