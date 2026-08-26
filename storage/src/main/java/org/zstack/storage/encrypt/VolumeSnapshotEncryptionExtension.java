package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.TakeSnapshotOnHypervisorMsg;
import org.zstack.header.storage.snapshot.BeforeTakeLiveSnapshotsOnVolumes;
import org.zstack.header.storage.snapshot.ConsistentType;
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotsJobStruct;
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotOverlayInnerMsg;
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct;
import org.zstack.header.storage.snapshot.TakeVolumesSnapshotOnKvmMsg;
import org.zstack.header.storage.snapshot.TakeVolumesSnapshotOnKvmReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotAfterDeleteExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotCreationExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.volume.CreateVolumeSnapshotGroupMessage;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMTakeSnapshotExtensionPoint;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.VolumeTO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

public class VolumeSnapshotEncryptionExtension implements KVMTakeSnapshotExtensionPoint,
        BeforeTakeLiveSnapshotsOnVolumes, VolumeSnapshotCreationExtensionPoint,
        VolumeSnapshotAfterDeleteExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotEncryptionExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;
    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;

    @Override
    public void beforeTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg,
                                   KVMAgentCommands.TakeSnapshotCmd cmd, Completion completion) {
        String snapshotUuid = null;
        try {
            VolumeVO volume = findVolume(msg.getVolume().getUuid());
            if (!volume.isEncrypted()) {
                completion.success();
                return;
            }

            VolumeSnapshotInventory snapshot = findSnapshot(msg.getSnapshotName());
            snapshotUuid = snapshot.getUuid();
            snapshotEncryptionHelper.inheritVolumeKeyToSnapshot(volume, snapshot);
            if (!cmd.isOnline()) {
                String envelopeDek = volumeEncryptedSecretHelper.materializeAndSealVolumeDekForHost(host.getUuid(), volume.getUuid());
                if (StringUtils.isNotBlank(envelopeDek)) {
                    cmd.setEncryptedDek(envelopeDek);
                }
            }

            completion.success();
        } catch (OperationFailureException e) {
            cleanupSnapshotKeyRef(snapshotUuid, "encrypted snapshot preparation failed");
            completion.fail(e.getErrorCode());
        } catch (RuntimeException e) {
            cleanupSnapshotKeyRef(snapshotUuid, "encrypted snapshot preparation failed");
            completion.fail(operr("failed to prepare encrypted volume snapshot[uuid:%s] on host[uuid:%s]: %s",
                    snapshotUuid, host.getUuid(), e.getMessage()));
        }
    }

    @Override
    public void beforeTakeLiveSnapshotsOnVolumes(CreateVolumesSnapshotOverlayInnerMsg msg,
                                                 TakeVolumesSnapshotOnKvmMsg tmsg,
                                                 Map flowData,
                                                 Completion completion) {
        List<String> inheritedSnapshotUuids = new ArrayList<>();
        try {
            if (tmsg == null || tmsg.getSnapshotJobs() == null) {
                completion.success();
                return;
            }

            Map<String, VolumeVO> volumes = findVolumes(tmsg.getSnapshotJobs());
            Map<String, VolumeSnapshotInventory> snapshots = findSnapshots(tmsg.getSnapshotJobs());
            for (TakeSnapshotsOnKvmJobStruct job : tmsg.getSnapshotJobs()) {
                if (job.isMemory()) {
                    continue;
                }

                VolumeVO volume = findVolume(volumes, job.getVolumeUuid());
                if (!volume.isEncrypted()) {
                    continue;
                }

                VolumeSnapshotInventory snapshot = findSnapshot(snapshots, job.getSnapshotUuid());
                snapshotEncryptionHelper.inheritVolumeKeyToSnapshot(volume, snapshot);
                inheritedSnapshotUuids.add(snapshot.getUuid());

                if (StringUtils.isBlank(job.getEncryptedDek())) {
                    String envelopeDek = volumeEncryptedSecretHelper.materializeAndSealVolumeDekForHost(tmsg.getHostUuid(), volume.getUuid());
                    if (StringUtils.isNotBlank(envelopeDek)) {
                        job.setEncryptedDek(envelopeDek);
                    }
                }

                if (job.getVolume() instanceof VolumeTO) {
                    ((VolumeTO) job.getVolume()).setLuksSecretUuid(
                            volumeEncryptedSecretHelper.resolveOrDefineSecretForVolume(
                                    tmsg.getHostUuid(), volume.getVmInstanceUuid(), volume.getUuid()));
                }
            }

            completion.success();
        } catch (OperationFailureException e) {
            cleanupSnapshotKeyRefs(inheritedSnapshotUuids, "encrypted live snapshot preparation failed");
            completion.fail(e.getErrorCode());
        } catch (RuntimeException e) {
            cleanupSnapshotKeyRefs(inheritedSnapshotUuids, "encrypted live snapshot preparation failed");
            completion.fail(operr("failed to prepare encrypted live volume snapshots").withException(e));
        }
    }

    @Override
    public void afterVolumeSnapshotCreated(VolumeSnapshotInventory snapshot, Completion completion) {
        try {
            if (snapshot == null) {
                completion.success();
                return;
            }

            VolumeVO volume = findVolume(snapshot.getVolumeUuid());
            if (!volume.isEncrypted()) {
                completion.success();
                return;
            }

            snapshotEncryptionHelper.completeTakeSnapshot(volume, snapshot);
            completion.success();
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        } catch (RuntimeException e) {
            completion.fail(operr("failed to complete encrypted volume snapshot[uuid:%s]: %s",
                    snapshot == null ? null : snapshot.getUuid(), e.getMessage()));
        }
    }

    @Override
    public void afterTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg,
                                  KVMAgentCommands.TakeSnapshotCmd cmd,
                                  KVMAgentCommands.TakeSnapshotResponse rsp) {
    }

    @Override
    public void afterTakeSnapshotFailed(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg,
                                        KVMAgentCommands.TakeSnapshotCmd cmd,
                                        KVMAgentCommands.TakeSnapshotResponse rsp,
                                        org.zstack.header.errorcode.ErrorCode err) {
        cleanupSnapshotKeyRef(msg == null ? null : msg.getSnapshotName(), "take snapshot failed on backend");
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreatedOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg,
                                                             TakeVolumesSnapshotOnKvmReply treply,
                                                             Completion completion) {
        completion.success();
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreationFailsOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg,
                                                                   TakeVolumesSnapshotOnKvmReply treply) {
        if (msg == null || msg.getVolumeSnapshotJobs() == null) {
            return;
        }
        List<String> snapshotUuids = new ArrayList<>();
        for (CreateVolumesSnapshotsJobStruct job : msg.getVolumeSnapshotJobs()) {
            if (job != null && StringUtils.isNotBlank(job.getResourceUuid())) {
                snapshotUuids.add(job.getResourceUuid());
            }
        }
        cleanupSnapshotKeyRefs(snapshotUuids, "live snapshot group creation failed on backend");
    }

    @Override
    public void afterVolumeSnapshotGroupCreated(VolumeSnapshotGroupInventory snapshotGroup,
                                                ConsistentType consistentType,
                                                Completion completion) {
        completion.success();
    }

    @Override
    public List<Flow> beforeCreateVolumeSnapshotFlow(CreateVolumeSnapshotGroupMessage msg) {
        return null;
    }

    @Override
    public void volumeSnapshotAfterDeleteExtensionPoint(VolumeSnapshotInventory snapshot, Completion completion) {
        if (snapshot != null && Boolean.TRUE.equals(snapshot.getEncrypted())) {
            cleanupSnapshotKeyRef(snapshot.getUuid(), "volume snapshot deleted");
        }
        completion.success();
    }

    @Override
    public void volumeSnapshotAfterFailedDeleteExtensionPoint(VolumeSnapshotInventory snapshot) {
        if (snapshot == null || StringUtils.isBlank(snapshot.getUuid())) {
            return;
        }
        if (!dbf.isExist(snapshot.getUuid(), VolumeSnapshotVO.class)) {
            cleanupSnapshotKeyRef(snapshot.getUuid(), "volume snapshot delete failed but DB row is gone");
        }
    }

    @Override
    public void volumeSnapshotAfterCleanUpExtensionPoint(String volumeUuid, List<VolumeSnapshotInventory> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        for (VolumeSnapshotInventory snapshot : snapshots) {
            if (snapshot == null || StringUtils.isBlank(snapshot.getUuid())) {
                continue;
            }
            if (!Boolean.TRUE.equals(snapshot.getEncrypted())) {
                continue;
            }
            if (dbf.isExist(snapshot.getUuid(), VolumeSnapshotVO.class)) {
                continue;
            }
            try {
                volumeEncryptedResourceKeyBackend.detachKeyProviderFromSnapshot(snapshot.getUuid());
            } catch (RuntimeException e) {
                logger.warn(String.format(
                        "failed to detach EncryptedResourceKeyRefVO for volume snapshot[uuid:%s] on delete cleanup: %s",
                        snapshot.getUuid(), e.getMessage()));
            }
        }
    }

    private void cleanupSnapshotKeyRefs(List<String> snapshotUuids, String reason) {
        if (snapshotUuids == null || snapshotUuids.isEmpty()) {
            return;
        }
        for (String snapshotUuid : snapshotUuids) {
            cleanupSnapshotKeyRef(snapshotUuid, reason);
        }
    }

    private void cleanupSnapshotKeyRef(String snapshotUuid, String reason) {
        if (StringUtils.isBlank(snapshotUuid)) {
            return;
        }
        try {
            if (volumeEncryptedResourceKeyBackend.checkSnapshotKeyProviderAttached(snapshotUuid)) {
                volumeEncryptedResourceKeyBackend.detachKeyProviderFromSnapshot(snapshotUuid);
                logger.debug(String.format(
                        "detached EncryptedResourceKeyRefVO for volume snapshot[uuid:%s], reason: %s",
                        snapshotUuid, reason));
            }
        } catch (RuntimeException e) {
            logger.warn(String.format(
                    "failed to detach EncryptedResourceKeyRefVO for volume snapshot[uuid:%s], reason: %s, error: %s",
                    snapshotUuid, reason, e.getMessage()));
        }
    }

    private VolumeVO findVolume(String volumeUuid) {
        if (StringUtils.isBlank(volumeUuid)) {
            throw new OperationFailureException(operr("volume uuid is required for encrypted snapshot preparation"));
        }

        VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
        if (volume == null) {
            throw new OperationFailureException(operr("volume[uuid:%s] not found", volumeUuid));
        }
        return volume;
    }

    private Map<String, VolumeVO> findVolumes(List<TakeSnapshotsOnKvmJobStruct> jobs) {
        Set<String> volumeUuids = new HashSet<>();
        for (TakeSnapshotsOnKvmJobStruct job : jobs) {
            if (!job.isMemory() && StringUtils.isNotBlank(job.getVolumeUuid())) {
                volumeUuids.add(job.getVolumeUuid());
            }
        }

        Map<String, VolumeVO> ret = new HashMap<>();
        if (volumeUuids.isEmpty()) {
            return ret;
        }

        for (VolumeVO volume : dbf.listByPrimaryKeys(volumeUuids, VolumeVO.class)) {
            ret.put(volume.getUuid(), volume);
        }
        return ret;
    }

    private VolumeVO findVolume(Map<String, VolumeVO> volumes, String volumeUuid) {
        if (StringUtils.isBlank(volumeUuid)) {
            throw new OperationFailureException(operr("volume uuid is required for encrypted snapshot preparation"));
        }

        VolumeVO volume = volumes.get(volumeUuid);
        if (volume == null) {
            throw new OperationFailureException(operr("volume[uuid:%s] not found", volumeUuid));
        }
        return volume;
    }

    private VolumeSnapshotInventory findSnapshot(String snapshotUuid) {
        if (StringUtils.isBlank(snapshotUuid)) {
            throw new OperationFailureException(operr("snapshot uuid is required for encrypted snapshot preparation"));
        }

        VolumeSnapshotVO snapshot = dbf.findByUuid(snapshotUuid, VolumeSnapshotVO.class);
        if (snapshot == null) {
            throw new OperationFailureException(operr("volume snapshot[uuid:%s] not found", snapshotUuid));
        }
        return VolumeSnapshotInventory.valueOf(snapshot);
    }

    private Map<String, VolumeSnapshotInventory> findSnapshots(List<TakeSnapshotsOnKvmJobStruct> jobs) {
        Set<String> snapshotUuids = new HashSet<>();
        for (TakeSnapshotsOnKvmJobStruct job : jobs) {
            if (!job.isMemory() && StringUtils.isNotBlank(job.getSnapshotUuid())) {
                snapshotUuids.add(job.getSnapshotUuid());
            }
        }

        Map<String, VolumeSnapshotInventory> ret = new HashMap<>();
        if (snapshotUuids.isEmpty()) {
            return ret;
        }

        for (VolumeSnapshotVO snapshot : dbf.listByPrimaryKeys(snapshotUuids, VolumeSnapshotVO.class)) {
            ret.put(snapshot.getUuid(), VolumeSnapshotInventory.valueOf(snapshot));
        }
        return ret;
    }

    private VolumeSnapshotInventory findSnapshot(Map<String, VolumeSnapshotInventory> snapshots, String snapshotUuid) {
        if (StringUtils.isBlank(snapshotUuid)) {
            throw new OperationFailureException(operr("snapshot uuid is required for encrypted snapshot preparation"));
        }

        VolumeSnapshotInventory snapshot = snapshots.get(snapshotUuid);
        if (snapshot == null) {
            throw new OperationFailureException(operr("volume snapshot[uuid:%s] not found", snapshotUuid));
        }
        return snapshot;
    }
}
