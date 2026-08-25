package org.zstack.header.volume;

import org.zstack.header.vm.VmInstanceSpec.ImageSpec;

/**
 * Created by xing5 on 2016/8/22.
 */
public class InstantiateRootVolumeMsg extends InstantiateVolumeMsg implements VolumeMessage {
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
