package org.zstack.header.storage.primary;

import org.zstack.header.vm.VmInstanceSpec.ImageSpec;

public class InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg extends InstantiateVolumeOnPrimaryStorageMsg implements PrimaryStorageMessage {
    private ImageSpec templateSpec;
    private boolean encryptedVolumeBackupRestore;

    public ImageSpec getTemplateSpec() {
        return templateSpec;
    }

    public void setTemplateSpec(ImageSpec templateSpec) {
        this.templateSpec = templateSpec;
    }

    public boolean isEncryptedVolumeBackupRestore() {
        return encryptedVolumeBackupRestore;
    }

    public void setEncryptedVolumeBackupRestore(boolean encryptedVolumeBackupRestore) {
        this.encryptedVolumeBackupRestore = encryptedVolumeBackupRestore;
    }
}
