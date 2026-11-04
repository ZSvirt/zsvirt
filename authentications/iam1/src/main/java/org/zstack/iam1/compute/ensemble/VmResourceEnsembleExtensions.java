package org.zstack.iam1.compute.ensemble;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.vm.VmAttachVolumeExtensionPoint;
import org.zstack.header.vm.VmInstanceAttachNicExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.cdrom.CdRomAfterCreateExtensionPoint;
import org.zstack.header.vm.cdrom.VmCdRomInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.iam1.header.ensemble.ResourceEnsembleInfo;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/08/27
 */
public class VmResourceEnsembleExtensions implements
        VmAttachVolumeExtensionPoint,
        CdRomAfterCreateExtensionPoint,
        VmInstanceAttachNicExtensionPoint,
        VolumeSnapshotCreationExtensionPoint {
    @Autowired
    private EnsembleExtensions extensions;

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        // do-nothing
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {
        // do-nothing
    }

    @Override
    public void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume) {
        // do-nothing
    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        if (volume.isShareable()) {
            return;
        }
        extensions.changeResourceEnsemble(vm.getUuid(), list(volume.getUuid()));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {
        // do-nothing
    }

    @Override
    public void afterCreateCdRom(VmCdRomInventory cdrom) {
        extensions.changeResourceEnsemble(cdrom.getVmInstanceUuid(), list(cdrom.getUuid()));
    }

    @Override
    public void afterAttachNicToVm(VmNicInventory vmNic) {
        extensions.changeResourceEnsemble(vmNic.getVmInstanceUuid(), list(vmNic.getUuid()));
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreatedOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply, Completion completion) {
        completion.success();
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreationFailsOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply) {

    }

    @Override
    public void afterVolumeSnapshotGroupCreated(VolumeSnapshotGroupInventory snapshotGroup, ConsistentType consistentType, Completion completion) {
        completion.success();
    }

    @Override
    public void afterVolumeSnapshotCreated(VolumeSnapshotInventory snapshot, Completion completion) {
        String volumeUuid = snapshot.getVolumeUuid();

        ResourceEnsembleInfo ensemble = ResourceEnsembleHelper.findResourceEnsemble(volumeUuid);
        if (ensemble == null) {
            completion.success();
            return;
        }

        extensions.changeResourceEnsemble(ensemble.uuid, list(snapshot.getUuid()));
        completion.success();
    }
}
