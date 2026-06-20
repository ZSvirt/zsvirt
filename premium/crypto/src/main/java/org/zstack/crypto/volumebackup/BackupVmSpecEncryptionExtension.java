package org.zstack.crypto.volumebackup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.BuildVmSpecExtensionPoint;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmInstanceSpec;

public class BackupVmSpecEncryptionExtension implements BuildVmSpecExtensionPoint {
    @Autowired
    private BackupImageKeyManager temporaryImageKeyManager;

    @Override
    public void afterBuildVmSpec(VmInstanceSpec spec) {
        temporaryImageKeyManager.ensureDiskTemporaryImageKey(spec.getRootDisk());
        for (DiskAO disk : spec.getTemplateDeprecatedDisksSpecs()) {
            temporaryImageKeyManager.ensureDiskTemporaryImageKey(disk);
        }
    }
}
