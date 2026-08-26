package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.MigrateVmOnHypervisorMsg;
import org.zstack.header.storage.migration.KvmMigrateVmWithStorageExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class VolumeEncryptedMigrateVmWithStorageExtension implements KvmMigrateVmWithStorageExtensionPoint {
    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;

    @Override
    public ErrorCode beforeMigrateVmWithStorage(HostInventory dstHost,
                                                VmInstanceInventory vm,
                                                List<VolumeVO> volumesToMigrate,
                                                Map<String, String> volumeMappingDict,
                                                MigrateVmOnHypervisorMsg msg) {
        Map<String, String> volumeLuksSecrets = new HashMap<>();

        try {
            for (VolumeVO sourceVolume : volumesToMigrate) {
                if (!sourceVolume.isEncrypted()) {
                    continue;
                }

                String targetVolumeUuid = volumeMappingDict.get(sourceVolume.getUuid());
                if (targetVolumeUuid == null || targetVolumeUuid.trim().isEmpty()) {
                    return operr("missing live migration temporary volume mapping for encrypted source volume[uuid:%s]",
                            sourceVolume.getUuid());
                }

                volumeLuksSecrets.put(targetVolumeUuid,
                        volumeEncryptedSecretHelper.resolveOrDefineSecretForVolume(
                                dstHost.getUuid(), vm.getUuid(), sourceVolume.getUuid()));
            }
        } catch (Exception e) {
            return operr("failed to prepare LUKS secrets for live storage migration vm[uuid:%s]: %s",
                    vm.getUuid(), e.getMessage());
        }

        if (!volumeLuksSecrets.isEmpty()) {
            msg.setVolumeLuksSecrets(volumeLuksSecrets);
        }
        return null;
    }

    @Override
    public String prepareVolumeEncryptedDek(String hostUuid, VolumeInventory volume) {
        if (volume == null || !Boolean.TRUE.equals(volume.getEncrypted())) {
            return null;
        }

        return volumeEncryptedSecretHelper.materializeAndSealVolumeDekForHost(hostUuid, volume.getUuid());
    }
}
