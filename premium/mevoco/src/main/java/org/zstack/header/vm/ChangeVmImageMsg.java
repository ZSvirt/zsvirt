package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 11/2/17.
 */
public class ChangeVmImageMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String imageUuid;
    private String resourceUuid;
    private boolean encryptedVolumeBackupRestore;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getResourceUuid() {
    	return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
    	this.resourceUuid = resourceUuid;
    }

    public boolean isEncryptedVolumeBackupRestore() {
        return encryptedVolumeBackupRestore;
    }

    public void setEncryptedVolumeBackupRestore(boolean encryptedVolumeBackupRestore) {
        this.encryptedVolumeBackupRestore = encryptedVolumeBackupRestore;
    }
}
