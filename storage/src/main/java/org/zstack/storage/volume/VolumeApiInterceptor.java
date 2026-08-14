package org.zstack.storage.volume;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.configuration.DiskOfferingSystemTags;
import org.zstack.configuration.OfferingUserConfigUtils;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfig;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageState;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.PrimaryStorageAllocateConfig;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.header.storage.snapshot.ConsistentType;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.group.MemorySnapshotValidatorExtensionPoint;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.APIAttachDataVolumeToHostMsg;
import org.zstack.header.volume.APIAttachDataVolumeToVmMsg;
import org.zstack.header.volume.APIBackupDataVolumeMsg;
import org.zstack.header.volume.APIChangeVolumeStateMsg;
import org.zstack.header.volume.APICreateDataVolumeFromVolumeTemplateMsg;
import org.zstack.header.volume.APICreateDataVolumeMsg;
import org.zstack.header.volume.APICreateVolumeSnapshotGroupMsg;
import org.zstack.header.volume.APICreateVolumeSnapshotMsg;
import org.zstack.header.volume.APIDeleteDataVolumeEvent;
import org.zstack.header.volume.APIDeleteDataVolumeMsg;
import org.zstack.header.volume.APIDetachDataVolumeFromHostMsg;
import org.zstack.header.volume.APIDetachDataVolumeFromVmMsg;
import org.zstack.header.volume.APIFlattenVolumeMsg;
import org.zstack.header.volume.APIGetDataVolumeAttachableVmMsg;
import org.zstack.header.volume.APIRecoverDataVolumeMsg;
import org.zstack.header.volume.APIUndoSnapshotCreationMsg;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeCreateMessage;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeHostRefVO;
import org.zstack.header.volume.VolumeHostRefVO_;
import org.zstack.header.volume.VolumeMessage;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:02 PM
 * To change this template use File | Settings | File Templates.
 */
@InterceptorForService("volume")
public class VolumeApiInterceptor implements Component, GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VolumeMessage) {
            VolumeMessage vmsg = (VolumeMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, vmsg.getVolumeUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof VolumeCreateMessage) {
            validate((VolumeCreateMessage) msg);
        }
        if (msg instanceof APIChangeVolumeStateMsg) {
            validate((APIChangeVolumeStateMsg) msg);
        } else if (msg instanceof APIDeleteDataVolumeMsg) {
            validate((APIDeleteDataVolumeMsg) msg);
        } else if (msg instanceof APICreateDataVolumeMsg) {
            validate((APICreateDataVolumeMsg) msg);
        } else if (msg instanceof APIBackupDataVolumeMsg) {
            validate((APIBackupDataVolumeMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToVmMsg) {
            validate((APIAttachDataVolumeToVmMsg) msg);
        } else if (msg instanceof APIDetachDataVolumeFromVmMsg) {
            validate((APIDetachDataVolumeFromVmMsg) msg);
        } else if (msg instanceof APIGetDataVolumeAttachableVmMsg) {
            validate((APIGetDataVolumeAttachableVmMsg) msg);
        } else if (msg instanceof APICreateDataVolumeFromVolumeTemplateMsg) {
            validate((APICreateDataVolumeFromVolumeTemplateMsg) msg);
        } else if (msg instanceof APIRecoverDataVolumeMsg) {
            validate((APIRecoverDataVolumeMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotGroupMsg) {
            validate((APICreateVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotMsg) {
            validate((APICreateVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToHostMsg) {
            validate((APIAttachDataVolumeToHostMsg) msg);
        } else if (msg instanceof APIDetachDataVolumeFromHostMsg) {
            validate((APIDetachDataVolumeFromHostMsg) msg);
        } else if (msg instanceof APIFlattenVolumeMsg) {
            validate((APIFlattenVolumeMsg) msg);
        } else if (msg instanceof APIUndoSnapshotCreationMsg) {
            validate((APIUndoSnapshotCreationMsg) msg);
        } else if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(VolumeCreateMessage msg) {
        String diskOffering = msg.getDiskOfferingUuid();
        if (diskOffering == null || !DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(diskOffering)) {
            return;
        }

        DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(diskOffering, DiskOfferingUserConfig.class);
        if (config.getAllocate() == null) {
            return;
        }

        if (!config.getAllocate().getAllPrimaryStorages().isEmpty()) {
            List<String> requiredPrimaryStorageUuids = config.getAllocate().getAllPrimaryStorages().stream()
                    .map(PrimaryStorageAllocateConfig::getUuid).collect(Collectors.toList());
            if (msg.getPrimaryStorageUuid() != null && !requiredPrimaryStorageUuids.contains(msg.getPrimaryStorageUuid())) {
                throw new ApiMessageInterceptionException(operr("primary storage uuid conflict, the primary storage specified by the disk offering are %s, and the primary storage specified in the creation parameter is %s",
                        requiredPrimaryStorageUuids, msg.getPrimaryStorageUuid()));
            }
        }
    }

    private void validate(APICreateVolumeSnapshotMsg msg) {
        Tuple tuple = Q.New(VolumeVO.class)
                .select(VolumeVO_.status, VolumeVO_.type, VolumeVO_.state)
                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                .findTuple();
        VolumeStatus status = (VolumeStatus) tuple.get(0);
        if (status != VolumeStatus.Ready) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is not in status Ready, current is %s, can't create snapshot", msg.getVolumeUuid(), status));
        }

        VolumeType type = (VolumeType) tuple.get(1);
        if (type != VolumeType.Root && type != VolumeType.Data) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s, type:%s], can't create snapshot", msg.getVolumeUuid(), type));
        }

        VolumeState state = (VolumeState) tuple.get(2);
        if (state != VolumeState.Enabled) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is not in state Enabled, " +
                    "current is %s, can't create snapshot", msg.getVolumeUuid(), state));
        }
    }

    private void validate(APICreateVolumeSnapshotGroupMsg msg) {
        VmInstanceVO vmvo = SQL.New("select vm from VmInstanceVO vm, VolumeVO vol" +
                " where vol.uuid = :volUuid" +
                " and vol.type = :volType" +
                " and vm.uuid = vol.vmInstanceUuid", VmInstanceVO.class)
                .param("volType", VolumeType.Root)
                .param("volUuid", msg.getRootVolumeUuid())
                .find();

        if (vmvo == null) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] is not root volume", msg.getRootVolumeUuid()));
        }

        if (msg.isWithMemory() && !(vmvo.getState().equals(VmInstanceState.Running) || (vmvo.getState().equals(VmInstanceState.Paused)))) {
            throw new ApiMessageInterceptionException(argerr("Can not take memory snapshot, vm current state[%s], but expect state are [%s, %s]",
                    vmvo.getState().toString(), VmInstanceState.Running.toString(), VmInstanceState.Paused.toString()));
        }

        for (VolumeVO vol : vmvo.getAllVolumes()) {
            if (vol.getStatus() != VolumeStatus.Ready) {
                throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is not in status Ready, " +
                        "current is %s, can't create snapshot", vol.getUuid(), vol.getStatus()));
            }
        }

        msg.setVmInstance(VmInstanceInventory.valueOf(vmvo));
        if (msg.isWithMemory()) {
            msg.setConsistentType(ConsistentType.Application);
        }

        for (MemorySnapshotValidatorExtensionPoint ext : pluginRgty.getExtensionList(MemorySnapshotValidatorExtensionPoint.class)) {
            if (!msg.isWithMemory()) {
                break;
            }

            ErrorCode errorCode = ext.checkVmWhereMemorySnapshotExistExternalDevices(msg.getVmInstance().getUuid());
            if (errorCode != null) {
                throw new ApiMessageInterceptionException(errorCode);
            }
        }
    }

    private void validate(APIRecoverDataVolumeMsg msg) {
        if (!Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                .eq(VolumeVO_.status, VolumeStatus.Deleted)
                .isExists()) {
            throw new ApiMessageInterceptionException(operr("the volume[uuid:%s] is not in status of deleted. This is operation is to recover a deleted data volume",
                    msg.getVolumeUuid()));
        }
    }

    private void exceptionIsVolumeIsDeleted(String volumeUuid) {
        if (Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, volumeUuid)
                .eq(VolumeVO_.status, VolumeStatus.Deleted)
                .isExists()) {
            throw new ApiMessageInterceptionException(operr("the volume[uuid:%s] is in status of deleted, cannot do the operation", volumeUuid));
        }
    }

    private void validate(APICreateDataVolumeFromVolumeTemplateMsg msg) {
        ImageVO img = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        ImageMediaType type = img.getMediaType();
        if (ImageMediaType.DataVolumeTemplate != type) {
            throw new ApiMessageInterceptionException(argerr("image[uuid:%s] is not %s, it's %s", msg.getImageUuid(), ImageMediaType.DataVolumeTemplate, type));
        }

        if (ImageState.Enabled != img.getState()) {
            throw new ApiMessageInterceptionException(operr("image[uuid:%s] is not Enabled, it's %s", img.getUuid(), img.getState()));
        }

        if (ImageStatus.Ready != img.getStatus()) {
            throw new ApiMessageInterceptionException(operr("image[uuid:%s] is not Ready, it's %s", img.getUuid(), img.getStatus()));
        }
    }

    private void validate(APIGetDataVolumeAttachableVmMsg msg) {
        Tuple t = Q.New(VolumeVO.class)
                .select(VolumeVO_.vmInstanceUuid, VolumeVO_.state, VolumeVO_.status, VolumeVO_.type)
                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                .findTuple();

        VolumeType type = t.get(3, VolumeType.class);
        if (type == VolumeType.Root) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] is Root volume, can not be attach to vm", msg.getVolumeUuid()));
        }

        // As per issue #1696, we do not report error if the volume has been attached.
        // Instead, an empty list will be returned later when handling this message.
        VolumeState state = t.get(1, VolumeState.class);
        if (state != VolumeState.Enabled) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] is in state[%s], data volume can only be attached when state is %s", msg.getVolumeUuid(), state, VolumeState.Enabled));
        }

        VolumeStatus status = t.get(2, VolumeStatus.class);
        if (status != VolumeStatus.Ready && status != VolumeStatus.NotInstantiated) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] is in status[%s], data volume can only be attached when status is %s or %S", msg.getVolumeUuid(), status, VolumeStatus.Ready, VolumeStatus.NotInstantiated));
        }
    }

    private void validate(APIDetachDataVolumeFromVmMsg msg) {
        VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (!vol.isShareable() && vol.getVmInstanceUuid() == null) {
            throw new ApiMessageInterceptionException(operr("data volume[uuid:%s] is not attached to any vm, can't detach", msg.getVolumeUuid()));
        }

        if (vol.isShareable() && msg.getVmUuid() == null) {
            throw new ApiMessageInterceptionException(operr("to detach shareable data volume[uuid:%s], vm uuid is needed.", msg.getVolumeUuid()));
        }


        if (vol.getType() != VolumeType.Data) {
            throw new ApiMessageInterceptionException(operr("the volume[uuid:%s, name:%s, type:%s] can't detach it",
                    vol.getUuid(), vol.getName(), vol.getType()));
        }
    }

    private void validate(APIAttachDataVolumeToVmMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<String> vmInstanceClusterUuids = new ArrayList<>();

                String clusterUuid = q(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).select(VmInstanceVO_.clusterUuid).findValue();
                if (clusterUuid != null) {
                    vmInstanceClusterUuids.add(clusterUuid);
                }

                if (vmInstanceClusterUuids.isEmpty()) {
                    String vmRootVolumeUuid = q(VmInstanceVO.class).select(VmInstanceVO_.rootVolumeUuid)
                            .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).findValue();

                    String vmPrimaryStorageUuid = q(VolumeVO.class).select(VolumeVO_.primaryStorageUuid)
                            .eq(VolumeVO_.uuid, vmRootVolumeUuid).findValue();

                    vmInstanceClusterUuids = q(PrimaryStorageClusterRefVO.class)
                            .select(PrimaryStorageClusterRefVO_.clusterUuid)
                            .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, vmPrimaryStorageUuid)
                            .listValues();
                }

                long count = sql("select count(vm.uuid)" +
                        " from VmInstanceVO vm" +
                        " where vm.uuid = :vmUuid" +
                        " and vm.platform = :platformType" +
                        " and vm.state != :vmState")
                        .param("vmUuid", msg.getVmInstanceUuid())
                        .param("vmState", VmInstanceState.Stopped)
                        .param("platformType", ImagePlatform.Other.toString()).find();
                if (count > 0) {
                    throw new ApiMessageInterceptionException(operr("the vm[uuid:%s] doesn't support to online attach volume[%s] on the basis of that the image platform type of the vm is other ", msg.getVmInstanceUuid(), msg.getVolumeUuid()));
                }

                String hvType = q(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).select(VmInstanceVO_.hypervisorType).findValue();
                String hostUuid = q(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).select(VmInstanceVO_.hostUuid).findValue();
                long attachedDataVolumeNum = q(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Data).eq(VolumeVO_.vmInstanceUuid, msg.getVmInstanceUuid()).count();
                VolumeVO volumeVO = q(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();

                ErrorCode error = checkDataVolume(volumeVO, hvType, attachedDataVolumeNum + 1);
                if (error != null) {
                    throw new ApiMessageInterceptionException(error);
                }

                error = checkClusterAccessible(volumeVO, vmInstanceClusterUuids);
                if (error != null) {
                    throw new ApiMessageInterceptionException(error);
                }

                error = checkHostAccessible(volumeVO, hostUuid);
                if (error != null) {
                    throw new ApiMessageInterceptionException(error);
                }

                List<SystemTagVO> systemTags = sql("select tag from SystemTagVO tag " +
                        "join VolumeVO v on tag.resourceUuid = v.uuid " +
                        "where tag.resourceType = :resourceType " +
                        "and (v.vmInstanceUuid = :vmInstanceUuid or v.uuid = :volumeUuid)")
                        .param("resourceType",VolumeVO.class.getSimpleName())
                        .param("vmInstanceUuid", msg.getVmUuid())
                        .param("volumeUuid", msg.getVolumeUuid())
                        .list();

                if (systemTags != null && !systemTags.isEmpty()) {
                    long scsiVolumeCount = systemTags.stream()
                            .filter(tag -> "capability::scsi".equals(tag.getTag()))
                            .count();

                    long virtioScsiVolumeCount = systemTags.stream()
                            .filter(tag -> "capability::virtio-scsi".equals(tag.getTag()))
                            .count();

                    if (scsiVolumeCount > 0 && virtioScsiVolumeCount > 0) {
                        throw new ApiMessageInterceptionException(operr("vm do not support having both SCSI and Virtio-SCSI bus type volumes simultaneously."));
                    }
                }
            }
        }.execute();
    }

    private ErrorCode checkDataVolume(VolumeVO volumeVO, String hvType, long attachedDataVolumeNum) {
        if (volumeVO.getType() == VolumeType.Root) {
            return operr("the volume[uuid:%s, name:%s] is Root Volume, can't attach it", volumeVO.getUuid(), volumeVO.getName());
        }

        if (volumeVO.getState() == VolumeState.Disabled) {
            return operr("data volume[uuid:%s] is Disabled, can't attach", volumeVO.getUuid());
        }

        if (volumeVO.getStatus() == VolumeStatus.Deleted) {
            return operr("the volume[uuid:%s] is in status of deleted, cannot do the operation", volumeVO.getUuid());
        }

        if (volumeVO.isAttached() && !volumeVO.isShareable()) {
            return operr("data volume[uuid:%s] has been attached to some vm, can't attach again", volumeVO.getUuid());
        }

        if (VolumeStatus.Ready != volumeVO.getStatus() && VolumeStatus.NotInstantiated != volumeVO.getStatus()) {
            return operr("data volume can only be attached when status is [%s, %s], current is %s",
                    VolumeStatus.Ready, VolumeStatus.NotInstantiated, volumeVO.getStatus());
        }

        if (volumeVO.getFormat() != null && hvType != null) {
            List<String> hvTypes = VolumeFormat.valueOf(volumeVO.getFormat()).getHypervisorTypesSupportingThisVolumeFormatInString();

            if (hvTypes.isEmpty()) {
                return operr("data volume[uuid:%s] of format[%s] is not supported for attach to any hypervisor.", volumeVO.getUuid(), volumeVO.getFormat());
            }

            if (!hvTypes.contains(hvType)) {
                return operr("data volume[uuid:%s] has format[%s] that can only be attached to hypervisor[%s], " +
                        "but vm has hypervisor type[%s]. Can't attach", volumeVO.getUuid(), volumeVO.getFormat(), hvTypes, hvType);
            }
        }

        return null;
    }

    private ErrorCode checkClusterAccessible(VolumeVO volumeVO, List<String> vmInstanceClusterUuids) {
        if (CollectionUtils.isEmpty(vmInstanceClusterUuids)) {
            return null;
        }

        List<String> volumeClusterUuids = Q.New(PrimaryStorageClusterRefVO.class)
                .select(PrimaryStorageClusterRefVO_.clusterUuid)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, volumeVO.getPrimaryStorageUuid())
                .listValues();

        vmInstanceClusterUuids.retainAll(volumeClusterUuids);

        // if there is no cluster contains both vm root volume and data volume, the data volume won't be attachable
        if (vmInstanceClusterUuids.isEmpty() && !volumeClusterUuids.isEmpty()) {
            return operr("Can't attach volume to VM, no qualified cluster");
        }

        return null;
    }

    private ErrorCode checkHostAccessible(VolumeVO volumeVO, String hostUuid) {
        if (hostUuid == null) {
            return null;
        }

        PrimaryStorageHostStatus primaryStorageHostStatus = Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, volumeVO.getPrimaryStorageUuid())
                .select(PrimaryStorageHostRefVO_.status)
                .findValue();
        if (primaryStorageHostStatus == PrimaryStorageHostStatus.Disconnected) {
            return operr("Can not attach volume to vm runs on host[uuid: %s] which is disconnected with volume's storage[uuid: %s]",
                    hostUuid, volumeVO.getPrimaryStorageUuid());
        }

        return null;
    }

    private void validate(APIBackupDataVolumeMsg msg) {
        if (isRootVolume(msg.getUuid())) {
            throw new ApiMessageInterceptionException(operr("it's not allowed to backup root volume, uuid:%s", msg.getUuid()));
        }

        exceptionIsVolumeIsDeleted(msg.getVolumeUuid());
    }

    private void validate(APICreateDataVolumeMsg msg) {
        if (msg.getDiskOfferingUuid() == null) {
            if (msg.getDiskSize() < 0) {
                throw new ApiMessageInterceptionException(argerr("unexpected disk size settings"));
            }
        } else {
            Long diskSize = Q.New(DiskOfferingVO.class).eq(DiskOfferingVO_.uuid, msg.getDiskOfferingUuid()).select(DiskOfferingVO_.diskSize).findValue();
            msg.setDiskSize(diskSize);
        }
    }

    private void validate(APIDeleteDataVolumeMsg msg) {
        if (!dbf.isExist(msg.getUuid(), VolumeVO.class)) {
            APIDeleteDataVolumeEvent evt = new APIDeleteDataVolumeEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        Tuple t = Q.New(VolumeVO.class)
                .select(VolumeVO_.type, VolumeVO_.status)
                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                .findTuple();
        VolumeType type = t.get(0, VolumeType.class);
        if (type != VolumeType.Data) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s, type:%s] can't be deleted", msg.getVolumeUuid(), type));
        }

        VolumeStatus status = t.get(1, VolumeStatus.class);
        if (status == VolumeStatus.Deleted) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is already in status of deleted", msg.getVolumeUuid()));
        }

        String hostUuid = Q.New(VolumeHostRefVO.class).select(VolumeHostRefVO_.hostUuid)
                .eq(VolumeHostRefVO_.volumeUuid, msg.getUuid()).findValue();
        if (hostUuid != null) {
            throw new ApiMessageInterceptionException(argerr("can not delete volume[%s], " +
                    "because volume attach to host[%s]", msg.getVolumeUuid(), hostUuid));
        }
    }

    private boolean isRootVolume(String uuid) {
        VolumeType type = Q.New(VolumeVO.class)
                .select(VolumeVO_.type)
                .eq(VolumeVO_.uuid, uuid)
                .findValue();
        return type == VolumeType.Root;
    }

    private void validate(APIChangeVolumeStateMsg msg) {
        if (isRootVolume(msg.getUuid())) {
            throw new ApiMessageInterceptionException(operr("it's not allowed to change state of root volume, uuid:%s", msg.getUuid()));
        }

        exceptionIsVolumeIsDeleted(msg.getVolumeUuid());

        String hostUuid = Q.New(VolumeHostRefVO.class).select(VolumeHostRefVO_.hostUuid)
                .eq(VolumeHostRefVO_.volumeUuid, msg.getUuid()).findValue();
        if (hostUuid != null) {
            throw new ApiMessageInterceptionException(argerr("can not change volume[%s] state, " +
                    "because volume attach to host[%s]", msg.getVolumeUuid(), hostUuid));
        }
    }

    private void validate(APIAttachDataVolumeToHostMsg msg) {
        HostStatus hostStatus = Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, msg.getHostUuid()).findValue();
        if (hostStatus != HostStatus.Connected) {
            throw new ApiMessageInterceptionException(operr("can not attach volume[%s] to host[%s], because host[status:%s] is not connected",
                    msg.getVolumeUuid(), msg.getHostUuid(), hostStatus));
        }

        if (!msg.getMountPath().startsWith("/")) {
            throw new ApiMessageInterceptionException(argerr("mount path must be absolute path"));
        }

        Tuple hostAndMountPath = Q.New(VolumeHostRefVO.class)
                .eq(VolumeHostRefVO_.volumeUuid, msg.getVolumeUuid())
                .select(VolumeHostRefVO_.hostUuid, VolumeHostRefVO_.mountPath).findTuple();
        if (hostAndMountPath != null) {
            doValidateAttachedVolume(hostAndMountPath, msg);
        } else {
            checkMountPathOnHost(msg);
        }
    }

    private void doValidateAttachedVolume(Tuple hostAndMountPath, APIAttachDataVolumeToHostMsg msg) {
        String hostUuid = hostAndMountPath.get(0, String.class);
        String mountPath = hostAndMountPath.get(1, String.class);
        if (!hostUuid.equals(msg.getHostUuid())) {
            throw new ApiMessageInterceptionException(operr("can not attach volume[%s] to host[%s], because volume is attaching to host[%s]",
                    msg.getVolumeUuid(), msg.getHostUuid(), hostUuid));
        }
        if (!mountPath.equals(msg.getMountPath())) {
            throw new ApiMessageInterceptionException(operr("can not attach volume[%s] to host[%s], because the volume[%s] occupies the mount path[%s] on host[%s]",
                    msg.getVolumeUuid(), msg.getHostUuid(), msg.getVolumeUuid(), mountPath, hostUuid));
        }
    }

    private void checkMountPathOnHost(APIAttachDataVolumeToHostMsg msg) {
        List<String> mountPaths = Q.New(VolumeHostRefVO.class)
                .eq(VolumeHostRefVO_.hostUuid, msg.getHostUuid())
                .select(VolumeHostRefVO_.mountPath).listValues();
        if (mountPaths.contains(msg.getMountPath())) {
            throw new ApiMessageInterceptionException(operr("can not attach volume[%s] to host[%s], because the another volume occupies the mount path[%s]",
                    msg.getVolumeUuid(), msg.getHostUuid(), msg.getMountPath()));
        }
    }

    private void validate(APIDetachDataVolumeFromHostMsg msg) {
        if (!Q.New(VolumeHostRefVO.class).eq(VolumeHostRefVO_.volumeUuid, msg.getVolumeUuid()).isExists()) {
            throw new ApiMessageInterceptionException(operr("can not detach volume[%s] from host. " +
                    "it may have been detached", msg.getVolumeUuid()));
        }
    }

    private void validate(APIFlattenVolumeMsg msg) {
        boolean isShareable = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).select(VolumeVO_.isShareable).findValue();
        if (isShareable) {
            throw new ApiMessageInterceptionException(argerr("cannot flatten a shareable volume[uuid:%s]", msg.getVolumeUuid()));
        }
    }

    private void validate(APIUndoSnapshotCreationMsg msg) {
        String currentTreeUuid = Q.New(VolumeSnapshotTreeVO.class)
                .select(VolumeSnapshotTreeVO_.uuid)
                .eq(VolumeSnapshotTreeVO_.current, true)
                .eq(VolumeSnapshotTreeVO_.volumeUuid, msg.getUuid())
                .findValue();
        if (currentTreeUuid == null) {
            throw new ApiMessageInterceptionException(operr("can not found in used snapshot tree of volume[uuid: %s]", msg.getUuid()));
        }

        boolean isLatest = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, msg.getSnapShotUuid())
                .eq(VolumeSnapshotVO_.latest, true)
                .eq(VolumeSnapshotVO_.treeUuid, currentTreeUuid)
                .isExists();

        if (!isLatest) {
            throw new ApiMessageInterceptionException(argerr("cannot undo not latest snapshot"));
        }
    }

    @Transactional
    protected void validate(APICreateVmInstanceMsg msg) {
        if (CollectionUtils.isEmpty(msg.getDiskAOs())) {
            return;
        }

        List<String> volumeUuids = msg.getDiskAOs().stream()
                .filter(diskAO -> Objects.equals(diskAO.getSourceType(), VolumeVO.class.getSimpleName()))
                .map(DiskAO::getSourceUuid).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(volumeUuids)) {
            return;
        }

        List<String> duplicateVolumeUuids = volumeUuids.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(duplicateVolumeUuids)) {
            throw new ApiMessageInterceptionException(operr("duplicate volume uuids: %s", duplicateVolumeUuids.toString()));
        }

        List<String> clusterUuids = new ArrayList<>();
        String hypervisorType = null;
        if (msg.getHostUuid() != null) {
            Tuple t = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).select(HostVO_.clusterUuid, HostVO_.hypervisorType).findTuple();
            clusterUuids.add(t.get(0, String.class));
            hypervisorType = t.get(1, String.class);
        } else if (msg.getClusterUuid() != null) {
            clusterUuids.add(msg.getClusterUuid());
            hypervisorType = Q.New(ClusterVO.class).eq(ClusterVO_.uuid, msg.getClusterUuid()).select(ClusterVO_.hypervisorType).findValue();
        }

        List<ErrorCode> errors = new ArrayList<>();

        List<VolumeVO> volumeVOs = Q.New(VolumeVO.class).in(VolumeVO_.uuid, volumeUuids).list();
        for (VolumeVO volume : volumeVOs) {
            ErrorCode error = checkDataVolume(volume, hypervisorType, volumeUuids.size());
            if (error != null) {
                errors.add(error);
                continue;
            }

            error = checkClusterAccessible(volume, clusterUuids);
            if (error != null) {
                errors.add(error);
                continue;
            }

            error = checkHostAccessible(volume, msg.getHostUuid());
            if (error != null) {
                errors.add(error);
            }
        }

        if (errors.size() == 1) {
            throw new ApiMessageInterceptionException(errors.get(0));
        } else if (errors.size() > 1) {
            throw new ApiMessageInterceptionException(argerr("invalid disk states").withCause(errors));
        }
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }


    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APICreateVmInstanceMsg.class,
            APICreateVolumeSnapshotGroupMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.FRONT;
    }

}
