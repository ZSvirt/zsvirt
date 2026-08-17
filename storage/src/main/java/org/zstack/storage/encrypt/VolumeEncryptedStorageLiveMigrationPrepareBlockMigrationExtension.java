package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.migration.KvmBlockLiveMigrationExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class VolumeEncryptedStorageLiveMigrationPrepareBlockMigrationExtension implements KvmBlockLiveMigrationExtensionPoint {
    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;

    @Override
    public ErrorCode beforeBlockLiveMigration(HostInventory dstHost,
                                              VmInstanceInventory vm,
                                              List<VolumeVO> volumesToMigrate,
                                              Map<String, String> volumeMappingDict,
                                              Map<String, String> targetVolumeLuksSecrets) {
        if (volumeMappingDict == null || volumeMappingDict.isEmpty()) {
            return null;
        }

        try {
            for (VolumeVO sourceVolume : volumesToMigrate) {
                if (!sourceVolume.isEncrypted()) {
                    continue;
                }

                String targetVolumeUuid = volumeMappingDict.get(sourceVolume.getUuid());
                if (targetVolumeUuid == null || targetVolumeUuid.trim().isEmpty()) {
                    return operr("missing target volume uuid for encrypted source volume[uuid:%s]",
                            sourceVolume.getUuid());
                }

                targetVolumeLuksSecrets.put(targetVolumeUuid,
                        volumeEncryptedSecretHelper.resolveOrDefineSecretForVolume(
                                dstHost.getUuid(), vm.getUuid(), sourceVolume.getUuid()));
            }
        } catch (Exception e) {
            return operr("failed to prepare LUKS secret for block live migration vm[uuid:%s]: %s",
                    vm.getUuid(), e.getMessage());
        }

        return null;
    }
}
