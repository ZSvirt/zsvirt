package org.zstack.externalbackup;

public class VolumeExternalBackupInfo extends ResourceExternalBackupInfo {
    protected String vmInstanceUuid;
    protected String type;
    protected long size;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
