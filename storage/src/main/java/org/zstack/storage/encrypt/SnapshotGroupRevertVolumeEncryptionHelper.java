package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupMsg;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.volume.CreateDataVolumeFromVolumeSnapshotMsg;
import org.zstack.header.volume.VolumeType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SnapshotGroupRevertVolumeEncryptionHelper {
    public ErrorCode validateVolumeSnapshotEncryption(APICreateVmInstanceFromVolumeSnapshotGroupMsg msg,
                                                      List<VolumeSnapshotVO> effectiveSnapshots) {
        Map<String, Boolean> volumeSnapshotEncryption;
        try {
            volumeSnapshotEncryption = getVolumeSnapshotEncryption(msg);
        } catch (IllegalArgumentException e) {
            return Platform.operr(e.getMessage());
        }
        if (volumeSnapshotEncryption == null || volumeSnapshotEncryption.isEmpty()) {
            boolean hasEncrypted = effectiveSnapshots.stream().anyMatch(VolumeSnapshotVO::isEncrypted);
            if (hasEncrypted) {
                return Platform.operr("volumeSnapshotEncryptions must specify every effective volume snapshot in volume snapshot group[uuid:%s]",
                        msg.getVolumeSnapshotGroupUuid());
            }
            return null;
        }

        Set<String> effectiveSnapshotUuids = new HashSet<>();
        for (VolumeSnapshotVO snapshot : effectiveSnapshots) {
            effectiveSnapshotUuids.add(snapshot.getUuid());
        }

        Set<String> unexpectedSnapshotUuids = new HashSet<>(volumeSnapshotEncryption.keySet());
        unexpectedSnapshotUuids.removeAll(effectiveSnapshotUuids);
        if (!unexpectedSnapshotUuids.isEmpty()) {
            return Platform.operr("volumeSnapshotEncryptions contain volume snapshot(s)%s that do not belong to volume snapshot group[uuid:%s]",
                    unexpectedSnapshotUuids, msg.getVolumeSnapshotGroupUuid());
        }

        Set<String> missingSnapshotUuids = new HashSet<>(effectiveSnapshotUuids);
        missingSnapshotUuids.removeAll(volumeSnapshotEncryption.keySet());
        if (!missingSnapshotUuids.isEmpty()) {
            return Platform.operr("volumeSnapshotEncryptions miss effective volume snapshot(s)%s in volume snapshot group[uuid:%s]",
                    missingSnapshotUuids, msg.getVolumeSnapshotGroupUuid());
        }

        for (VolumeSnapshotVO snapshot : effectiveSnapshots) {
            if (snapshot.isEncrypted() && !Boolean.TRUE.equals(volumeSnapshotEncryption.get(snapshot.getUuid()))) {
                return Platform.operr("volume snapshot[uuid:%s] in volume snapshot group[uuid:%s] is encrypted, cannot create an unencrypted volume from it",
                        snapshot.getUuid(), msg.getVolumeSnapshotGroupUuid());
            }
        }

        return null;
    }

    public void setupRootVolumeFromApi(APICreateVmInstanceFromVolumeSnapshotGroupMsg apiMsg,
                                       CreateVmInstanceMsg cmsg) {
        Map<String, Boolean> volumeSnapshotEncryption = getVolumeSnapshotEncryption(apiMsg);
        if (volumeSnapshotEncryption == null || volumeSnapshotEncryption.isEmpty()) {
            return;
        }
        if (cmsg.getRootDisk() == null) {
            return;
        }

        String rootSnapshotUuid = Q.New(VolumeSnapshotGroupRefVO.class).select(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid)
                .eq(VolumeSnapshotGroupRefVO_.volumeType, VolumeType.Root.toString())
                .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, apiMsg.getVolumeSnapshotGroupUuid())
                .findValue();
        cmsg.getRootDisk().setEncrypted(volumeSnapshotEncryption.get(rootSnapshotUuid));
    }

    public void setupDataVolumeFromApi(APICreateVmInstanceFromVolumeSnapshotGroupMsg apiMsg,
                                       VolumeSnapshotVO snapshot,
                                       CreateDataVolumeFromVolumeSnapshotMsg cmsg) {
        Map<String, Boolean> volumeSnapshotEncryption = getVolumeSnapshotEncryption(apiMsg);
        cmsg.setEncrypted(volumeSnapshotEncryption == null ? null : volumeSnapshotEncryption.get(snapshot.getUuid()));
    }

    private Map<String, Boolean> getVolumeSnapshotEncryption(APICreateVmInstanceFromVolumeSnapshotGroupMsg msg) {
        List<APICreateVmInstanceFromVolumeSnapshotGroupMsg.VolumeSnapshotEncryption> volumeSnapshotEncryptions =
                msg.getVolumeSnapshotEncryptions();
        if (volumeSnapshotEncryptions == null || volumeSnapshotEncryptions.isEmpty()) {
            return null;
        }

        Map<String, Boolean> ret = new HashMap<>();
        for (APICreateVmInstanceFromVolumeSnapshotGroupMsg.VolumeSnapshotEncryption volumeSnapshotEncryption :
                volumeSnapshotEncryptions) {
            if (volumeSnapshotEncryption.getVolumeSnapshotUuid() == null || volumeSnapshotEncryption.getEncrypted() == null) {
                throw new IllegalArgumentException(String.format(
                        "invalid volumeSnapshotEncryptions item[%s], expected {\"volumeSnapshotUuid\":\"snapshotUuid\",\"encrypted\":true}",
                        volumeSnapshotEncryption));
            }
            ret.put(volumeSnapshotEncryption.getVolumeSnapshotUuid(), volumeSnapshotEncryption.getEncrypted());
        }

        return ret;
    }
}
