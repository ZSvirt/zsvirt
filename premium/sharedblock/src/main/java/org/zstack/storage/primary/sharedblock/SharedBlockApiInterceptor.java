package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg;
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeSnapshotMsg;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storageDevice.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.kvm.KVMConstant;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO_;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.core.db.Q.New;

/**
 * Create by weiwang at 04/04/2018
 */
public class SharedBlockApiInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(SharedBlockApiInterceptor.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSharedBlockGroupPrimaryStorageMsg) {
            validate((APIAddSharedBlockGroupPrimaryStorageMsg) msg);
        } else if (msg instanceof APIResizeDataVolumeMsg) {
            validate((APIResizeDataVolumeMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotMsg) {
            validate((APICreateVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIDeleteVolumeSnapshotMsg) {
            validate((APIDeleteVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeSnapshotMsg) {
            validate((APICreateDataVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeMsg) {
            validate((APICreateDataVolumeTemplateFromVolumeMsg) msg);
        } else if (msg instanceof APICloneVmInstanceMsg) {
            validate((APICloneVmInstanceMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToVmMsg) {
            validate((APIAttachDataVolumeToVmMsg) msg);
        } else if (msg instanceof APIAttachScsiLunToVmInstanceMsg) {
            validate((APIAttachScsiLunToVmInstanceMsg) msg);
        } else if ((msg instanceof APIAttachDataVolumeToHostMsg)) {
            validate((APIAttachDataVolumeToHostMsg) msg);
        } else if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APIConvertVmInstanceToTemplatedVmInstanceMsg) {
            validate((APIConvertVmInstanceToTemplatedVmInstanceMsg) msg);
        } else if (msg instanceof APICreateTemplatedVmInstanceFromVmInstanceMsg) {
            validate((APICreateTemplatedVmInstanceFromVmInstanceMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateTemplatedVmInstanceFromVmInstanceMsg msg) {
        ensureVmWithoutScsiLun(msg.getVmInstanceUuid());
    }


    private void validate(APIConvertVmInstanceToTemplatedVmInstanceMsg msg) {
        ensureVmWithoutScsiLun(msg.getVmInstanceUuid());
    }

    private void ensureVmWithoutScsiLun(String vmInstanceUuid) {
        List<String> lunUuids = Q.New(ScsiLunVmInstanceRefVO.class)
                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, vmInstanceUuid)
                .select(ScsiLunVmInstanceRefVO_.scsiLunUuid)
                .listValues();
        if (!lunUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr(
                    "templated vm[uuid: %s] cannot be create from vm with scsi lun[uuids: %s]",
                    vmInstanceUuid, lunUuids));
        }
    }

    private void validate(APIAttachScsiLunToVmInstanceMsg msg) {
        ScsiLunVO scsiLunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, msg.getUuid()).find();
        SharedBlockVO sharedBlockVO = Q.New(SharedBlockVO.class).eq(SharedBlockVO_.diskUuid, scsiLunVO.getWwid()).find();

        if (sharedBlockVO != null) {
            throw new ApiMessageInterceptionException(argerr(
                    "primary storage[uuid: %s] has attached the scsi lun[wwid: %s]", sharedBlockVO.getSharedBlockGroupUuid(), scsiLunVO.getWwid()));
        }
    }

    private void validate(APIAttachDataVolumeToVmMsg msg) {
        if (!VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.hasTag(msg.getVolumeUuid(), VolumeVO.class)) {
            return;
        }


        String provisioningStrategyToken = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.getTokenByResourceUuid(
                msg.getVolumeUuid(), VolumeVO.class, VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN);
        if (!provisioningStrategyToken.equals(VolumeProvisioningStrategy.ThinProvisioning.toString())) {
            return;
        }

        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (!Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused).contains(vmInstanceVO.getState())) {
            return;
        }

        if (KVMConstant.KVM_HYPERVISOR_TYPE.equals(vmInstanceVO.getHypervisorType()) &&
                !VmSystemTags.ADDITIONAL_QMP_ADDED.hasTag(vmInstanceVO.getUuid())) {
            throw new ApiMessageInterceptionException(argerr(
                    "the vm[uuid: %s] does not has additional qmp socket, it may because of the vm start without " +
                            "the global config[vm.additionalQmp] enabled, please make sure it enabled and reboot vm in zstack",
                    msg.getVmInstanceUuid()
            ));
        }

    }

    private void validate(APICloneVmInstanceMsg msg) {
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        for (VolumeVO volumeVO : vmInstanceVO.getAllVolumes()) {
            if (msg.getFull().equals(false)) {
                continue;
            }

            if (!isSharedBlockGroupPrimaryStorage(volumeVO.getPrimaryStorageUuid())) {
                continue;
            }
        }
    }

    private void validate(APIAddSharedBlockGroupPrimaryStorageMsg msg) {
        // NOTE(weiw): no need to validate or format uuid since we actually support wwn
        if (msg.getDiskUuids() == null || msg.getDiskUuids().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(
                    "must specify at least one disk when add shared block group primary storage"
            ));
        }

        checkExistingPrimaryStorage(msg.getDiskUuids());
    }

    private void checkExistingPrimaryStorage(List<String> diskUuids) {
        for (String uuid : diskUuids) {
            SharedBlockVO vo = New(SharedBlockVO.class).eq(SharedBlockVO_.diskUuid, uuid).find();
            if (vo != null) {
                throw new ApiMessageInterceptionException(argerr(
                        "shared block[uuid:%s, diskUuid:%s, description:%s] already added to shared block group[uuid:%s]" +
                                "in new shared block group", vo.getUuid(), vo.getDiskUuid(), vo.getDescription(), vo.getSharedBlockGroupUuid()));
            }
        }
    }

    private void validate(APIResizeDataVolumeMsg msg) {
        VolumeVO volumeVO = New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getUuid()).find();
        if (volumeVO == null ) {
            throw new ApiMessageInterceptionException(argerr(
                    "can not find volume[uuid: %s]", msg.getUuid()
            ));
        }

        if (!New(SharedBlockGroupVO.class).eq(SharedBlockGroupVO_.uuid, volumeVO.getPrimaryStorageUuid()).isExists()) {
            return;
        }

        if (!volumeVO.isShareable()) {
            return;
        }

        throw new ApiMessageInterceptionException(argerr(
                "shared volume[uuid: %s] on shared block group primary storage can not resize", msg.getUuid()
        ));

//        List<String> attachedVminstanceUuids = Q.New(ShareableVolumeVmInstanceRefVO.class)
//                .select(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid)
//                .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, msg.getUuid())
//                .listValues();
//
//        if (attachedVminstanceUuids != null && !attachedVminstanceUuids.isEmpty()) {
//            throw new ApiMessageInterceptionException(argerr(
//                    "shared volume[uuid: %s] has been attached to vm instances[uuids: %s], can not resize",
//                    msg.getUuid(), attachedVminstanceUuids
//            ));
//        }
    }

    private void validate(APICreateVolumeSnapshotMsg msg) {
        avoidLiveChangeMetaDataOnSharedVolume(msg.getVolumeUuid());
    }

    private void validate(APIDeleteVolumeSnapshotMsg msg) {
        VolumeSnapshotVO snapshotVO = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, msg.getUuid()).find();
        avoidLiveChangeMetaDataOnSharedVolume(snapshotVO.getVolumeUuid());
    }

    private void validate(APICreateDataVolumeTemplateFromVolumeMsg msg) {
        avoidLiveChangeMetaDataOnSharedVolume(msg.getVolumeUuid());
    }

    private void validate(APICreateDataVolumeTemplateFromVolumeSnapshotMsg msg) {
        String volumeUuid = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, msg.getSnapshotUuid())
                .select(VolumeSnapshotVO_.volumeUuid)
                .find();
        avoidLiveChangeMetaDataOnSharedVolume(volumeUuid);
    }

    private void avoidLiveChangeMetaDataOnSharedVolume(String volumeUuid) {
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).find();
        if (volumeVO == null) {
            return;
        }

        if (!isSharedBlockGroupPrimaryStorage(volumeVO.getPrimaryStorageUuid())) {
            return;
        }

        if (!volumeVO.isShareable()) {
            return;
        }

        List<ShareableVolumeVmInstanceRefVO> refVOS = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, volumeUuid).list();

        if (refVOS == null || refVOS.isEmpty()) {
            return;
        }

        List<String> vmInstanceUuids = refVOS.stream().map(ShareableVolumeVmInstanceRefVO::getVmInstanceUuid).collect(Collectors.toList());
        List<String> notStoppedVmUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .notEq(VmInstanceVO_.state, VmInstanceState.Stopped)
                .in(VmInstanceVO_.uuid, vmInstanceUuids).listValues();

        if (notStoppedVmUuids == null || notStoppedVmUuids.isEmpty()) {
            return;
        }

        throw new ApiMessageInterceptionException(argerr(
                "shared volume[uuid: %s] on shared block group primary storage has attached to not stopped vm instances[uuids: %s]",
                volumeUuid, notStoppedVmUuids
        ));
    }

    private static Boolean isSharedBlockGroupPrimaryStorage(String psUuid) {
        return Q.New(SharedBlockGroupVO.class)
                .eq(SharedBlockGroupVO_.uuid, psUuid)
                .isExists();
    }

    private void validate(APIAttachDataVolumeToHostMsg msg) {
        String sql = "select ps.type from PrimaryStorageVO ps, VolumeVO vo " +
                "where vo.primaryStorageUuid = ps.uuid and vo.uuid = :volumeUuid";
        String psType = SQL.New(sql, String.class).param("volumeUuid", msg.getVolumeUuid()).find();

        if (!Objects.equals(psType, SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE)) {
            return;
        }

        String volumeProvisioningStrategy = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY
                .getTokenByResourceUuid(msg.getVolumeUuid(), VolumeVO.class, VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN);

        if (volumeProvisioningStrategy == null) {
            throw new ApiMessageInterceptionException(
                    operr("can not find the preparation of the volume[%s]", msg.getVolumeUuid()));
        }

        if (!Objects.equals(volumeProvisioningStrategy, VolumeProvisioningStrategy.ThickProvisioning.toString())) {
            throw new ApiMessageInterceptionException(operr("use the thick provisioning volume as the cache volume. " +
                    "the preparation of the volume[%s] is %s", msg.getVolumeUuid(), volumeProvisioningStrategy));
        }
    }

    private void validate(APICreateVmInstanceMsg msg) {
        if (CollectionUtils.isEmpty(msg.getDiskAOs())) {
            return;
        }

        List<String> lunUuids = msg.getDiskAOs().stream()
                .filter(diskAO -> Objects.equals(diskAO.getSourceType(), LunVO.class.getSimpleName()))
                .map(DiskAO::getSourceUuid).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(lunUuids)) {
            return;
        }

        List<ErrorCode> errors = new ArrayList<>();

        String sql = "select lun.uuid,lun.wwid,block.sharedBlockGroupUuid from ScsiLunVO lun, SharedBlockVO block where " +
                "lun.wwid = block.diskUuid and lun.uuid in (:lunUuids)";
        List<Tuple> scsiLunTuples = SQL.New(sql, Tuple.class).param("lunUuids", lunUuids).list();
        scsiLunTuples.forEach(t ->
                errors.add(operr("the scsi lun[uuid: %s, wwid: %s] is already attach to primary storage[uuid: %s]",
                        t.get(0, String.class), t.get(1, String.class), t.get(2, String.class))));

        if (!errors.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(errors.toString()));
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return CollectionDSL.list(
                // former serviceConfig(storage.primary / sharedblock) bindings
                APIAddSharedBlockGroupPrimaryStorageMsg.class,
                APIAddSharedBlockToSharedBlockGroupMsg.class,
                APIRefreshSharedblockDeviceCapacityMsg.class,
                APIGetSharedBlockCandidateMsg.class,
                APIQuerySharedBlockGroupPrimaryStorageMsg.class,
                APIQuerySharedBlockMsg.class,
                APIQuerySharedBlockGroupPrimaryStorageHostRefMsg.class,
                APIUpdateSharedBlockMsg.class,
                // cross-service globals
                APIResizeDataVolumeMsg.class,
                APICreateVolumeSnapshotMsg.class,
                APIDeleteVolumeSnapshotMsg.class,
                APICreateDataVolumeTemplateFromVolumeMsg.class,
                APICreateDataVolumeTemplateFromVolumeSnapshotMsg.class,
                APICloneVmInstanceMsg.class,
                APIAttachDataVolumeToVmMsg.class,
                APIAttachScsiLunToVmInstanceMsg.class,
                APIAttachDataVolumeToHostMsg.class,
                APICreateVmInstanceMsg.class,
                APIConvertVmInstanceToTemplatedVmInstanceMsg.class,
                APICreateTemplatedVmInstanceFromVmInstanceMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
