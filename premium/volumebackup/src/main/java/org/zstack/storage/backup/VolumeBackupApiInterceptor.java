package org.zstack.storage.backup;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmInstanceHelper;
import org.zstack.compute.vm.VmNicManager;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.scheduler.*;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.volume.backup.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.scheduler.*;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSelector;
import org.zstack.utils.message.OperationChecker;

import javax.persistence.Tuple;
import java.util.*;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

@InterceptorForService("backup.volume")
public class VolumeBackupApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private SchedulerFacade schedulerFacade;
    @Autowired
    private VmNicManager nicManager;

    protected static OperationChecker allowedOperations = new OperationChecker(true);

    private static Set<String> backupSchedulerType = new HashSet<>();

    private static Set<String> backupSchedulerJobClassNames = new HashSet<>();

    static {
        allowedOperations.addState(BackupStorageState.Enabled,
                APICreateDataVolumeTemplateFromVolumeBackupMsg.class.getName(),
                APICreateRootVolumeTemplateFromVolumeBackupMsg.class.getName(),
                APICreateVolumeBackupMsg.class.getName(),
                APICreateVmBackupMsg.class.getName(),
                APIRecoverBackupFromImageStoreBackupStorageMsg.class.getName(),
                APIRecoverVmBackupFromImageStoreBackupStorageMsg.class.getName(),
                APISyncBackupFromImageStoreBackupStorageMsg.class.getName(),
                APISyncVmBackupFromImageStoreBackupStorageMsg.class.getName(),
                APIRevertVolumeFromVolumeBackupMsg.class.getName(),
                APIRevertVmFromVmBackupMsg.class.getName()
        );

        backupSchedulerType.add(SchedulerType.VM_BACKUP);
        backupSchedulerType.add(SchedulerType.VOLUME_BACKUP);
        backupSchedulerType.add(SchedulerType.ROOT_VOLUME_BACKUP);
        backupSchedulerType.add(SchedulerType.DATABASE_BACKUP);

        backupSchedulerJobClassNames.add(CreateVmBackupJob.class.getName());
        backupSchedulerJobClassNames.add(CreateVolumeBackupJob.class.getName());
        backupSchedulerJobClassNames.add(CreateRootVolumeBackupJob.class.getName());
        backupSchedulerJobClassNames.add(CreateDatabaseBackupJob.class.getName());

    }

    private ErrorCode validateOperationOnBackupStorage(OperationChecker checker, String msgName, String bsUuid) {
        BackupStorageState bsState = Q.New(BackupStorageVO.class).select(BackupStorageVO_.state).eq(BackupStorageVO_.uuid, bsUuid).findValue();
        return validateOperationByState(checker, msgName, bsState);
    }

    private ErrorCode validateOperationByState(OperationChecker checker, String msgName, BackupStorageState currentState) {
        if (checker.isOperationAllowed(msgName, currentState.toString())) {
            return null;
        } else {
            return operr("current backup storage state[%s] doesn't allow to proceed message[%s], allowed states are %s", currentState,
                    msgName, checker.getStatesForOperation(msgName));
        }
    }


    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);

        if (msg instanceof APICreateVolumeBackupMsg) {
            validate((APICreateVolumeBackupMsg) msg);
        } else if (msg instanceof APICreateVmBackupMsg) {
            validate((APICreateVmBackupMsg) msg);
        } else if (msg instanceof APICreateVmFromVmBackupMsg) {
            validate((APICreateVmFromVmBackupMsg) msg);
        } else if (msg instanceof APICreateVmFromVolumeBackupMsg) {
            validate((APICreateVmFromVolumeBackupMsg) msg);
        } else if (msg instanceof APIRevertVolumeFromVolumeBackupMsg) {
            validate((APIRevertVolumeFromVolumeBackupMsg) msg);
        } else if (msg instanceof APIRevertVmFromVmBackupMsg) {
            validate((APIRevertVmFromVmBackupMsg) msg);
        } else if (msg instanceof APIRecoverVmBackupFromImageStoreBackupStorageMsg) {
            validate((APIRecoverVmBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APISyncBackupFromImageStoreBackupStorageMsg) {
            validate((APISyncBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APISyncVmBackupFromImageStoreBackupStorageMsg) {
            validate((APISyncVmBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeBackupMsg) {
            validate((APICreateDataVolumeTemplateFromVolumeBackupMsg) msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromVolumeBackupMsg) {
            validate((APICreateRootVolumeTemplateFromVolumeBackupMsg) msg);
        } else if (msg instanceof APIRecoverBackupFromImageStoreBackupStorageMsg) {
            validate((APIRecoverBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APIDeleteVolumeBackupMsg) {
            validate((APIDeleteVolumeBackupMsg) msg);
        } else if (msg instanceof APIDeleteVmBackupMsg) {
            validate((APIDeleteVmBackupMsg) msg);
        } else if (msg instanceof APICreateDataVolumeFromVolumeBackupMsg) {
            validate((APICreateDataVolumeFromVolumeBackupMsg) msg);
        }

        return msg;
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof BackupStorageMessage) {
            BackupStorageMessage bmsg = (BackupStorageMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeBackupConstant.SERVICE_ID, bmsg.getBackupStorageUuid());
        }
    }

    private void validate(APIRecoverBackupFromImageStoreBackupStorageMsg msg) {
        ErrorCode errorCode = validateOperationOnBackupStorage(allowedOperations, msg.getMessageName(), msg.getDstBackupStorageUuid());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }

        errorCode = validateOperationOnBackupStorage(allowedOperations, msg.getMessageName(), msg.getSrcBackupStorageUuid());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    private void validate(APICreateRootVolumeTemplateFromVolumeBackupMsg msg) {
        ErrorCode errorCode = validateOperationOnBackupStorage(allowedOperations, msg.getMessageName(), msg.getBackupStorageUuid());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    private void validate(APICreateDataVolumeTemplateFromVolumeBackupMsg msg) {
        ErrorCode errorCode = validateOperationOnBackupStorage(allowedOperations, msg.getMessageName(), msg.getBackupStorageUuid());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    private void validate(APISyncVmBackupFromImageStoreBackupStorageMsg msg) {
        ErrorCode errorCode = validateOperationOnBackupStorage(allowedOperations, msg.getMessageName(), msg.getDstBackupStorageUuid());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }

        errorCode = validateOperationOnBackupStorage(allowedOperations, msg.getMessageName(), msg.getDstBackupStorageUuid());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    private void validate(APIRecoverVmBackupFromImageStoreBackupStorageMsg msg) {
        ErrorCode errorCode = validateOperationOnBackupStorage(allowedOperations, msg.getMessageName(), msg.getSrcBackupStorageUuid());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }

        errorCode = validateOperationOnBackupStorage(allowedOperations, msg.getMessageName(), msg.getSrcBackupStorageUuid());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    private void validate(APISyncBackupFromImageStoreBackupStorageMsg msg) {
        ErrorCode errorCode = validateOperationOnBackupStorage(allowedOperations, msg.getMessageName(), msg.getDstBackupStorageUuid());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }

        errorCode = validateOperationOnBackupStorage(allowedOperations, msg.getMessageName(), msg.getSrcBackupStorageUuid());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    private Tuple validateVolumeBackup(String bsUuid, String volumeUuid, String msgName) {
        Tuple tuple = Q.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.uuid, bsUuid)
                .select(BackupStorageVO_.type, BackupStorageVO_.state)
                .findTuple();

        String bsType = (String) tuple.get(0);
        BackupStorageState state = (BackupStorageState) tuple.get(1);

        if (!bsType.equals(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE)) {
            throw new ApiMessageInterceptionException(operr("Unexpected backup storage[type:%s,uuid:%s]",
                    bsType, bsUuid));
        }

        ErrorCode errorCode = validateOperationByState(allowedOperations, msgName, state);

        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }

        return Q.New(VolumeVO.class)
                .select(VolumeVO_.vmInstanceUuid, VolumeVO_.isShareable, VolumeVO_.type)
                .eq(VolumeVO_.uuid, volumeUuid)
                .findTuple();
    }

    private void validate(APICreateVolumeBackupMsg msg) {
        Tuple t = validateVolumeBackup(msg.getBackupStorageUuid(), msg.getVolumeUuid(), msg.getMessageName());
        boolean isShareable = (boolean) t.get(1);

        if (isShareable) {
            throw new ApiMessageInterceptionException(operr("Can not create volume backup for shareable volume[uuid:%s]", msg.getVolumeUuid()));
        }

        if (t.get(0) == null) {
            throw new ApiMessageInterceptionException(operr("Failed to create volume backup for volume[uuid:%s], because it is not attached to any vm",
                    msg.getVolumeUuid()));
        }

        if (!Q.New(VmInstanceVO.class).in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused)).eq(VmInstanceVO_.uuid, t.get(0)).isExists()) {
            if (t.get(2).equals(VolumeType.Root)) {
                throw new ApiMessageInterceptionException(operr("Failed to create volume backup for volume[uuid:%s], because the vm is not in state[%s, %s]",
                        msg.getVolumeUuid(), VmInstanceState.Running.toString(), VmInstanceState.Paused.toString()));
            } else {
                throw new ApiMessageInterceptionException(operr("Failed to create volume backup for volume[uuid:%s], because its attached volume is not in state[%s, %s]",
                        msg.getVolumeUuid(), VmInstanceState.Running.toString(), VmInstanceState.Paused.toString()));
            }
        }

        msg.setAccountUuid(msg.getSession().getAccountUuid());
    }

    private void validate(APICreateVmBackupMsg msg) {
        Tuple t = validateVolumeBackup(msg.getBackupStorageUuid(), msg.getVolumeUuid(), msg.getMessageName());
        if (!t.get(2).equals(VolumeType.Root)) {
            throw new ApiMessageInterceptionException(operr("Volume[uuid:%s] is not root volume", msg.getVolumeUuid()));
        }

        if (!Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused))
                .eq(VmInstanceVO_.uuid, t.get(0))
                .isExists()) {
            throw new ApiMessageInterceptionException(operr("Failed to create backups for VM[uuid:%s], because it is not in state[%s, %s]",
                    t.get(0), VmInstanceState.Running.toString(), VmInstanceState.Paused.toString()));
        }
    }

    private void validate(APIRevertVmFromVmBackupMsg msg) {
        final String groupUuid = msg.getGroupUuid();
        final String bsUuid = msg.getBackupStrogeUuid();

        List<Tuple> ts = Q.New(VolumeBackupVO.class)
                .eq(VolumeBackupVO_.groupUuid, groupUuid)
                .select(VolumeBackupVO_.uuid, VolumeBackupVO_.volumeUuid, VolumeBackupVO_.type, VolumeBackupVO_.vmInstanceUuid)
                .listTuple();
        if (ts.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("No volume backup found for group uuid: %s", groupUuid));
        }

        List<String> rootVolumeInfo = ts.stream().filter(it -> it.get(2, VolumeType.class) == VolumeType.Root)
                .map(t -> list(t.get(1, String.class), t.get(3, String.class)))
                .findFirst()
                .orElseThrow(() -> new ApiMessageInterceptionException(operr("root volume backup of group[uuid:%s] not found", groupUuid)));

        String expectVmUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, rootVolumeInfo.get(0)).select(VolumeVO_.vmInstanceUuid).findValue();
        if (expectVmUuid != null && rootVolumeInfo.get(1) != null && !expectVmUuid.equals(rootVolumeInfo.get(1))) {
            throw new ApiMessageInterceptionException(operr("Current vm[uuid: %s] of the volume[uuid: %s] is no longer the vm[uuid: %s] that was used for backup",
                    expectVmUuid, rootVolumeInfo.get(0), rootVolumeInfo.get(1)));
        }

        msg.setVmInstanceUuid(expectVmUuid == null ? rootVolumeInfo.get(1) : expectVmUuid);
        ts.forEach(t -> validateRevert(msg.getMessageName(), t.get(0, String.class), bsUuid, expectVmUuid));
    }

    private void validateZoneOrClusterOrHostOrL3Exist(APICreateVmFromVmBackupMsg msg) {
        if (CollectionUtils.isEmpty(msg.getL3NetworkUuids()) && StringUtils.isEmpty(msg.getZoneUuid())
                && StringUtils.isEmpty(msg.getClusterUuid()) && StringUtils.isEmpty(msg.getHostUuid())) {
            throw new ApiMessageInterceptionException(operr("could not create vm, because at least one of field (l3NetworkUuids,zoneUuid,clusterUuid,hostUuid) should be set"));
        }
    }

    private void validate(APICreateVmFromVmBackupMsg msg) {
        if (msg.getInstanceOfferingUuid() == null && (msg.getCpuNum() == null || msg.getMemorySize() == null)) {
            throw new ApiMessageInterceptionException(operr("instanceOfferingUuid or cpuNum and memorySize must be set"));
        }

        // deprecated fields are mutually exclusive with diskAOs;
        // diskAOs already carries per-disk primaryStorageUuid and systemTags
        if (!CollectionUtils.isEmpty(msg.getDiskAOs())) {
            List<String> conflictFields = new ArrayList<>();
            if (msg.getPrimaryStorageUuidForRootVolume() != null) {
                conflictFields.add("primaryStorageUuidForRootVolume");
            }
            if (msg.getPrimaryStorageUuidForDataVolume() != null) {
                conflictFields.add("primaryStorageUuidForDataVolume");
            }
            if (!CollectionUtils.isEmpty(msg.getRootVolumeSystemTags())) {
                conflictFields.add("rootVolumeSystemTags");
            }
            if (!CollectionUtils.isEmpty(msg.getDataVolumeSystemTags())) {
                conflictFields.add("dataVolumeSystemTags");
            }
            if (!conflictFields.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr(
                        "deprecated field(s) %s cannot be specified together with diskAOs;" +
                                " please put per-disk primaryStorageUuid/systemTags into diskAOs instead",
                        conflictFields));
            }

            // fail-fast on malformed diskAOs to avoid silent fallback to batch/metadata settings.
            // diskAOs are matched to backup bundles strictly via sourceUuid == VolumeBackupVO.volumeUuid,
            // regardless of boot flag; so sourceUuid must be present, unique, and belong to the backup group.
            Set<String> seen = new HashSet<>();
            for (DiskAO ao : msg.getDiskAOs()) {
                String src = ao.getSourceUuid();
                if (src == null) {
                    throw new ApiMessageInterceptionException(argerr(
                            "diskAOs[*].sourceUuid is required and must equal the original volume uuid of the backup"));
                }
                if (!seen.add(src)) {
                    throw new ApiMessageInterceptionException(argerr(
                            "duplicated diskAOs.sourceUuid: %s", src));
                }
            }

            if (!seen.isEmpty()) {
                List<String> groupVolumeUuids = Q.New(VolumeBackupVO.class)
                        .select(VolumeBackupVO_.volumeUuid)
                        .eq(VolumeBackupVO_.groupUuid, msg.getGroupUuid())
                        .listValues();
                Set<String> unknown = new HashSet<>(seen);
                unknown.removeAll(new HashSet<>(groupVolumeUuids));
                if (!unknown.isEmpty()) {
                    throw new ApiMessageInterceptionException(argerr(
                            "diskAOs.sourceUuid %s do not belong to backup group[uuid:%s]",
                            unknown, msg.getGroupUuid()));
                }
            }
        }

        if (!StringUtils.isEmpty(msg.getVmNicParams())) {
            String platform = SQL.New("select vm.platform from VmInstanceVO vm, VolumeBackupVO backup" +
                            " where backup.groupUuid = :groupUuid" +
                            " and backup.vmInstanceUuid = vm.uuid", String.class)
                    .param("groupUuid", msg.getGroupUuid())
                    .find();
            msg.setPlatform(platform);
        }

        new VmInstanceHelper().validate(msg);

        Set<String> psUuids = new HashSet<>();
        if (msg.getPrimaryStorageUuidForDataVolume() != null) {
            psUuids.add(msg.getPrimaryStorageUuidForDataVolume());
        }

        if (msg.getPrimaryStorageUuidForRootVolume() != null) {
            psUuids.add(msg.getPrimaryStorageUuidForRootVolume());
        }

        validateZoneOrClusterOrHostOrL3Exist(msg);

        if (psUuids.size() < 2) {
            return;
        }

        List<String> clusterUuids = SQL.New("select ref.clusterUuid from PrimaryStorageClusterRefVO ref" +
                " where ref.primaryStorageUuid in :psUuids" +
                " group by ref.clusterUuid" +
                " having count(distinct ref.primaryStorageUuid) = :size", String.class)
                .param("psUuids", psUuids)
                .param("size", (long) psUuids.size())
                .list();

        if (clusterUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("cannot specify primary storage which attached different cluster."));
        }
    }

    private void validate(APICreateVmFromVolumeBackupMsg msg) {
        VolumeBackupVO volumeBackupVO = Q.New(VolumeBackupVO.class).eq(VolumeBackupVO_.uuid, msg.getBackupUuid()).find();
        if (volumeBackupVO == null) {
            throw new ApiMessageInterceptionException(operr("cannot find volume backup[uuid:%s]", msg.getBackupUuid()));
        }

        if (volumeBackupVO.getType() != VolumeType.Root) {
            throw new ApiMessageInterceptionException(operr("cannot create vm from volume backup[uuid:%s] which is not root volume backup", msg.getBackupUuid()));
        }

        msg.setVolumeBackupVO(volumeBackupVO);

        if (msg.getInstanceOfferingUuid() == null && (msg.getCpuNum() == null || msg.getMemorySize() == null)) {
            throw new ApiMessageInterceptionException(argerr(
                    "either instanceOfferingUuid or cpuNum and memorySize must be specified"));
        }

        new VmInstanceHelper().validate(msg);
    }

    private void validate(APIRevertVolumeFromVolumeBackupMsg msg) {
        Tuple t = SQL.New("select vol.vmInstanceUuid, vol.uuid from VolumeVO vol, VolumeBackupVO backup" +
                " where backup.uuid = :backupUuid" +
                " and backup.volumeUuid = vol.uuid", Tuple.class)
                .param("backupUuid", msg.getUuid())
                .find();
        msg.setVolumeUuid(t.get(1, String.class));
        String bsUuid = validateRevert(msg.getMessageName(), msg.getUuid(), msg.getBackupStrogeUuid(), t.get(0, String.class));
        msg.setBackupStrogeUuid(bsUuid);
    }

    @Transactional(readOnly = true)
    private String validateRevert(String msgName, String backupUuid, String backupStorageUuid, String expectVmUuid) {
        Tuple t = Q.New(VolumeBackupVO.class)
                .select(VolumeBackupVO_.state, VolumeBackupVO_.volumeUuid)
                .eq(VolumeBackupVO_.uuid, backupUuid)
                .findTuple();
        VolumeBackupState state = t.get(0, VolumeBackupState.class);
        if (state != VolumeBackupState.Enabled) {
            throw new ApiMessageInterceptionException(operr("volume backup[uuid:%s] is in state %s, cannot revert volume to it", backupUuid, state));
        }

        String volUuid = t.get(1, String.class);
        if (volUuid == null) {
            throw new ApiMessageInterceptionException(operr("original volume for backup[uuid:%s] has been deleted, cannot revert volume to it", backupUuid));
        }

        Tuple volTs = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volUuid).select(VolumeVO_.type, VolumeVO_.vmInstanceUuid).findTuple();
        if (volTs == null) {
            throw new ApiMessageInterceptionException(operr("original volume for backup[uuid:%s] has been deleted, cannot revert volume to it", backupUuid));
        }

        if (!Objects.equals(expectVmUuid, volTs.get(1, String.class))) {
            throw new ApiMessageInterceptionException(operr("original volume[uuid:%s] for backup[uuid:%s] is no longer attached to vm[uuid:%s]",
                    volUuid, backupUuid, expectVmUuid));
        }

        VolumeType volumeType = volTs.get(0, VolumeType.class);
        VmInstanceState vmState = SQL.New("select vm.state from VmInstanceVO vm, VolumeVO vol "
                + "where vol.uuid = :volUuid and vm.uuid = vol.vmInstanceUuid", VmInstanceState.class)
                .param("volUuid", volUuid)
                .find();
        if (vmState == null && volumeType == VolumeType.Root) {
            throw new ApiMessageInterceptionException(operr("VM not found with volume backup[uuid:%s]", backupUuid));
        }

        if (vmState != VmInstanceState.Stopped && vmState != null) {
            throw new ApiMessageInterceptionException(operr("VM is not in stopped state: %s", vmState));
        }

        if (backupStorageUuid == null) {
            List<String> bsUuids = SQL.New("select bs.uuid from BackupStorageVO bs, VolumeBackupStorageRefVO ref "
                    + "where ref.volumeBackupUuid = :backupUuid "
                    + "and bs.uuid = ref.backupStorageUuid "
                    + "and bs.status = :status "
                    + "and bs.state = :state")
                    .param("backupUuid", backupUuid)
                    .param("status", BackupStorageStatus.Connected)
                    .param("state", BackupStorageState.Enabled)
                    .list();

            List<String> remoteUuids = ImageStoreBackupStorageSelector.getRemoteBsUuids();
            bsUuids.removeAll(remoteUuids);
            if (bsUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(operr("No available backup storage found"));
            }

            Collections.shuffle(bsUuids);
            backupStorageUuid = bsUuids.get(0);
        }

        ErrorCode errorCode = validateOperationOnBackupStorage(allowedOperations, msgName, backupStorageUuid);
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }

        return backupStorageUuid;
    }

    private void validate(APIDeleteVolumeBackupMsg msg) {
        VolumeBackupVO vo = Q.New(VolumeBackupVO.class).eq(VolumeBackupVO_.uuid, msg.getUuid()).find();
        if (vo != null) {
            msg.setInventory(VolumeBackupInventory.valueOf(vo));
        }
    }

    private void validate(APIDeleteVmBackupMsg msg) {
        List<VolumeBackupVO> vos = Q.New(VolumeBackupVO.class).eq(VolumeBackupVO_.groupUuid, msg.getGroupUuid()).list();
        msg.setInventories(VolumeBackupInventory.valueOf(vos));
    }

    private void validate(APICreateDataVolumeFromVolumeBackupMsg msg) {
        VolumeBackupVO volumeBackupVO = Q.New(VolumeBackupVO.class).eq(VolumeBackupVO_.uuid, msg.getBackupUuid()).find();
        if (volumeBackupVO == null) {
            throw new ApiMessageInterceptionException(operr("cannot find volume backup[uuid:%s]", msg.getBackupUuid()));
        }

        msg.setVolumeBackupVO(volumeBackupVO);

        if (msg.getPrimaryStorageUuid() != null && msg.getVmInstanceUuid() != null) {
            String sql = "select vm.clusterUuid from VmInstanceVO vm " +
                    "where vm.uuid = :vmUuid " +
                    "and vm.clusterUuid in (select ref.clusterUuid from PrimaryStorageClusterRefVO ref where ref.primaryStorageUuid = :psUuid)";
            List<String> clusterUuids = SQL.New(sql, String.class)
                    .param("psUuid", msg.getPrimaryStorageUuid()).param("vmUuid", msg.getVmInstanceUuid()).list();
            if (clusterUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(
                        operr("the cluster of vm[%s] is not in the same cluster as the primaryStorage[%s]", msg.getVmInstanceUuid(), msg.getPrimaryStorageUuid()));
            }
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APICreateSchedulerJobMsg.class,
                APICreateSchedulerJobGroupMsg.class
        );
    }
}
