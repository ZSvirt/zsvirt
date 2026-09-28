package org.zstack.storage.primary.sharedblock;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO_;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

class SharedBlockVolumeResizeValidator {
    private SharedBlockVolumeResizeValidator() {
    }

    static VolumeVO validateExtend(String volumeUuid, long requiredSize, String expectedPrimaryStorageUuid) {
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).find();
        if (volumeVO == null) {
            throw new OperationFailureException(operr("cannot find volume[uuid: %s]", volumeUuid));
        }

        if (volumeVO.getStatus() != VolumeStatus.Ready) {
            throw new OperationFailureException(operr(
                    "volume[uuid: %s] is not ready, current status is %s",
                    volumeUuid, volumeVO.getStatus()));
        }

        String primaryStorageUuid = volumeVO.getPrimaryStorageUuid();
        if (expectedPrimaryStorageUuid != null && !Objects.equals(expectedPrimaryStorageUuid, primaryStorageUuid)) {
            throw new OperationFailureException(operr(
                    "volume[uuid: %s] primary storage changed from[uuid: %s] to[uuid: %s] while waiting to extend",
                    volumeUuid, expectedPrimaryStorageUuid, primaryStorageUuid));
        }

        if (primaryStorageUuid == null ||
                !Q.New(SharedBlockGroupVO.class).eq(SharedBlockGroupVO_.uuid, primaryStorageUuid).isExists()) {
            throw new OperationFailureException(argerr(
                    "volume[uuid: %s] is not on a shared block group primary storage", volumeUuid));
        }

        if (requiredSize > volumeVO.getSize()) {
            throw new OperationFailureException(argerr(
                    "required size[%s] exceeds volume[uuid: %s] virtual size[%s]",
                    requiredSize, volumeUuid, volumeVO.getSize()));
        }

        validateOffline(volumeVO);
        return volumeVO;
    }

    private static void validateOffline(VolumeVO volumeVO) {
        String volumeUuid = volumeVO.getUuid();
        List<String> vmInstanceUuids = new ArrayList<>();
        if (volumeVO.isShareable()) {
            vmInstanceUuids = Q.New(ShareableVolumeVmInstanceRefVO.class)
                    .select(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid)
                    .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, volumeUuid)
                    .listValues();
        } else if (volumeVO.getVmInstanceUuid() != null) {
            vmInstanceUuids.add(volumeVO.getVmInstanceUuid());
        }

        if (vmInstanceUuids.isEmpty()) {
            return;
        }

        List<String> notStoppedVmUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .notEq(VmInstanceVO_.state, VmInstanceState.Stopped)
                .in(VmInstanceVO_.uuid, vmInstanceUuids)
                .listValues();
        if (!notStoppedVmUuids.isEmpty()) {
            throw new OperationFailureException(operr(
                    "cannot extend logical volume[uuid: %s] while attached vm instances[uuids: %s] are not stopped",
                    volumeUuid, notStoppedVmUuids));
        }
    }
}
