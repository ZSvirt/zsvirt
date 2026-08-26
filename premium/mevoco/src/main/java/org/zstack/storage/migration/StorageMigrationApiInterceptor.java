package org.zstack.storage.migration;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.IsoOperator;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO_;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.backup.CephBackupStorageVO;
import org.zstack.storage.ceph.backup.CephBackupStorageVO_;
import org.zstack.storage.migration.backup.APIBackupStorageMigrateImageMsg;
import org.zstack.storage.migration.backup.APIGetBackupStorageCandidatesForImageMigrationMsg;
import org.zstack.storage.migration.primary.APIGetHostCandidatesForVmMigrationMsg;
import org.zstack.storage.migration.primary.APIGetPrimaryStorageCandidatesForVmMigrationMsg;
import org.zstack.storage.migration.primary.APIGetPrimaryStorageCandidatesForVolumeMigrationMsg;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeMsg;
import org.zstack.storage.migration.primary.VolumeMigrationAO;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 9/1/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StorageMigrationApiInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(StorageMigrationApiInterceptor.class);

    @Autowired
    protected AccountManager acmgr;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRegistry;

    private Map<String, List<String>> backupStorageBackupStorageMetrics;
    private Map<String, List<String>> primaryStoragePrimaryStorageMetrics;

    public Map<String, List<String>> getBackupStorageBackupStorageMetrics() {
        return backupStorageBackupStorageMetrics;
    }

    public void setBackupStorageBackupStorageMetrics(Map<String, List<String>> backupStorageBackupStorageMetrics) {
        this.backupStorageBackupStorageMetrics = backupStorageBackupStorageMetrics;
    }

    public Map<String, List<String>> getPrimaryStoragePrimaryStorageMetrics() {
        return primaryStoragePrimaryStorageMetrics;
    }

    public void setPrimaryStoragePrimaryStorageMetrics(Map<String, List<String>> primaryStoragePrimaryStorageMetrics) {
        this.primaryStoragePrimaryStorageMetrics = primaryStoragePrimaryStorageMetrics;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIBackupStorageMigrateImageMsg) {
            validate((APIBackupStorageMigrateImageMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVolumeMsg) {
            validate((APIPrimaryStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVmMsg) {
            APIPrimaryStorageMigrateVmMsg mmsg = (APIPrimaryStorageMigrateVmMsg) msg;
            boolean legacyRootOnly = isLegacyRootOnly(mmsg);
            mmsg.setLegacyRootOnlyMigration(legacyRootOnly);
            normalize(mmsg);
            validate(mmsg, legacyRootOnly);
        }

        return msg;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
                APIBackupStorageMigrateImageMsg.class,
                APIGetBackupStorageCandidatesForImageMigrationMsg.class,
                APIPrimaryStorageMigrateVolumeMsg.class,
                APIPrimaryStorageMigrateVmMsg.class,
                APIGetPrimaryStorageCandidatesForVolumeMigrationMsg.class,
                APIGetPrimaryStorageCandidatesForVmMigrationMsg.class,
                APIGetHostCandidatesForVmMigrationMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }

    private void normalize(APIPrimaryStorageMigrateVmMsg msg) {
        if (msg.getVolumeMigrationAOs() != null && !msg.getVolumeMigrationAOs().isEmpty()) {
            return;
        }
        if (msg.getDstPrimaryStorageUuid() == null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "either volumeMigrationAOs or dstPrimaryStorageUuid is required"));
        }

        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vm == null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot find VM[uuid:%s]", msg.getVmInstanceUuid()));
        }

        msg.setVolumeMigrationAOs(VolumeMigrationAO.fromLegacyDstPs(
                vm, msg.getVolumeMigrationAOs(), msg.getDstPrimaryStorageUuid(), msg.isWithDataVolumes(), msg.isWithSnapshots()));
    }

    private boolean isLegacyRootOnly(APIPrimaryStorageMigrateVmMsg msg) {
        return (msg.getVolumeMigrationAOs() == null || msg.getVolumeMigrationAOs().isEmpty())
                && msg.getDstPrimaryStorageUuid() != null
                && !msg.isWithDataVolumes();
    }

    private boolean isFullVmMigration(VmInstanceVO vm, List<VolumeMigrationAO> specs) {
        if (specs == null || specs.isEmpty()) {
            return true;
        }
        Set<String> specVolUuids = new HashSet<>();
        for (VolumeMigrationAO s : specs) {
            specVolUuids.add(s.getVolumeUuid());
        }
        for (VolumeVO v : vm.getAllDiskVolumes()) {
            if (!specVolUuids.contains(v.getUuid())) {
                return false;
            }
        }
        return true;
    }

    private void validateVmStorageLayout(VmInstanceVO vm, List<VolumeMigrationAO> specs,
                                         String dstHostUuid, List<String> systemTags,
                                         boolean legacyRootOnly) {
        Map<String, String> dstPsByVolumeUuid = specs.stream().collect(Collectors.toMap(
                VolumeMigrationAO::getVolumeUuid, VolumeMigrationAO::getDstPrimaryStorageUuid));
        if (legacyRootOnly
                && specs.size() == 1
                && vm.getRootVolumeUuid().equals(specs.get(0).getVolumeUuid())
                && !StorageMigrationLayoutChecker.hasFinalLocalStorage(vm, dstPsByVolumeUuid)) {
            return;
        }
        String invalidReason = StorageMigrationLayoutChecker.validateFinalLayout(
                vm, dstPsByVolumeUuid, dstHostUuid, systemTags);
        if (invalidReason != null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot migrate VM[uuid:%s] because %s", vm.getUuid(), invalidReason));
        }
    }

    // do not migrate vm with iso in ceph backup storage attached
    private void checkAttachedISO(VmInstanceVO vm) {
        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(vm.getUuid());
        if (isoUuids == null || isoUuids.isEmpty()) {
            return;
        }

        if (new SQLBatchWithReturn<Boolean>() {
            @Override
            protected Boolean scripts() {
                List<String> bsUuids = q(ImageBackupStorageRefVO.class)
                        .in(ImageBackupStorageRefVO_.imageUuid, isoUuids)
                        .select(ImageBackupStorageRefVO_.backupStorageUuid)
                        .listValues();
                return q(CephBackupStorageVO.class)
                        .in(CephBackupStorageVO_.uuid, bsUuids)
                        .isExists();
            }
        }.execute()) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "do not support storage migration with iso in ceph backup storage attached"
            ));
        }
    }

    private void validate(APIPrimaryStorageMigrateVmMsg msg, boolean legacyRootOnly) {
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (vmInstanceVO == null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot find VM[uuid:%s]", msg.getVmInstanceUuid()));
        }

        checkAttachedISO(vmInstanceVO);

        List<VolumeMigrationAO> specs = msg.getVolumeMigrationAOs();

        List<String> shareableVolUuids = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vmInstanceVO.getUuid())
                .select(ShareableVolumeVmInstanceRefVO_.volumeUuid)
                .listValues();
        if (!shareableVolUuids.isEmpty()) {
            boolean specsHitShareable = specs != null && specs.stream()
                    .map(VolumeMigrationAO::getVolumeUuid)
                    .anyMatch(shareableVolUuids::contains);
            if (isFullVmMigration(vmInstanceVO, specs) || specsHitShareable) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "do not support storage migration of vm[uuid:%s, name: %s] while shared volume attached",
                        vmInstanceVO.getUuid(), vmInstanceVO.getName()));
            }
        }

        if (specs == null || specs.isEmpty()) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "volumeMigrationAOs is required"));
        }

        Set<String> seenVolUuids = new HashSet<>();
        for (VolumeMigrationAO s : specs) {
            if (s.getVolumeUuid() == null || s.getDstPrimaryStorageUuid() == null) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "volumeMigrationAOs entry has null volumeUuid or dstPrimaryStorageUuid"));
            }
            if (!seenVolUuids.add(s.getVolumeUuid())) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "duplicate volumeUuid[%s] in volumeMigrationAOs", s.getVolumeUuid()));
            }
        }

        Map<String, VolumeVO> vmVols = new HashMap<>();
        for (VolumeVO v : vmInstanceVO.getAllDiskVolumes()) {
            vmVols.put(v.getUuid(), v);
        }
        for (VolumeMigrationAO s : specs) {
            if (!vmVols.containsKey(s.getVolumeUuid())) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "volume[uuid:%s] is not a disk of VM[uuid:%s]",
                        s.getVolumeUuid(), vmInstanceVO.getUuid()));
            }
        }

        if (!vmInstanceVO.getState().equals(VmInstanceState.Stopped) && vmInstanceVO.getHostUuid() == null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "VM[uuid:%s] is %s but host uuid is missing",
                    vmInstanceVO.getUuid(), vmInstanceVO.getState()));
        }

        List<VmStorageMigrationMetric> metrics =
                new ArrayList<>(pluginRegistry.getExtensionList(VmStorageMigrationMetric.class));

        Map<String, PrimaryStorageVO> dstPsCache = new HashMap<>();
        for (VolumeMigrationAO spec : specs) {
            VolumeVO volumeVO = vmVols.get(spec.getVolumeUuid());

            if (volumeVO.getState() == VolumeState.Disabled) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "cannot migrate volume[uuid:%s] in state Disabled", volumeVO.getUuid()));
            }
            if (volumeVO.getStatus() != VolumeStatus.Ready && volumeVO.getStatus() != VolumeStatus.Migrating) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "cannot migrate volume[uuid:%s] in status[%s]", volumeVO.getUuid(), volumeVO.getStatus()));
            }

            PrimaryStorageVO dstPrimaryStorageVO = dstPsCache.computeIfAbsent(spec.getDstPrimaryStorageUuid(),
                    uuid -> Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, uuid).find());
            if (dstPrimaryStorageVO == null) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "destination primary storage[uuid:%s] does not exist", spec.getDstPrimaryStorageUuid()));
            }
            if (dstPrimaryStorageVO.getState() == PrimaryStorageState.Disabled
                    || dstPrimaryStorageVO.getState() == PrimaryStorageState.Maintenance) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "destination primary storage[uuid:%s] state is %s",
                        dstPrimaryStorageVO.getUuid(), dstPrimaryStorageVO.getState()));
            }
            if (dstPrimaryStorageVO.getStatus() != PrimaryStorageStatus.Connected) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "destination primary storage[uuid:%s] status is %s",
                        dstPrimaryStorageVO.getUuid(), dstPrimaryStorageVO.getStatus()));
            }

            PrimaryStorageVO srcPrimaryStorageVO = Q.New(PrimaryStorageVO.class)
                    .eq(PrimaryStorageVO_.uuid, volumeVO.getPrimaryStorageUuid())
                    .find();
            if (srcPrimaryStorageVO == null) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "source primary storage[uuid:%s] of volume[uuid:%s] not found",
                        volumeVO.getPrimaryStorageUuid(), volumeVO.getUuid()));
            }
            String srcPsType = srcPrimaryStorageVO.getType();
            String dstPsType = dstPrimaryStorageVO.getType();

            boolean capable = false;
            for (VmStorageMigrationMetric metric : metrics) {
                if (!metric.isCapable(srcPsType, dstPsType)) {
                    continue;
                }

                if (!metric.isDestinationPrimaryStorageSupported(volumeVO, dstPrimaryStorageVO.getUuid())) {
                    continue;
                }

                if ((vmInstanceVO.getState().equals(VmInstanceState.Running) || vmInstanceVO.getState().equals(VmInstanceState.Paused))
                        && !metric.isSupportLive()) {
                    continue;
                }

                if (vmInstanceVO.getState().equals(VmInstanceState.Stopped) && !metric.isSupportOffline()) {
                    continue;
                }

                if (volumeVO.getType() == VolumeType.Data && !metric.isSupportWithDataVolume()) {
                    continue;
                }

                if (spec.isWithSnapshots() && !metric.isSupportWithSnapshot()) {
                    continue;
                }

                capable = true;
                break;
            }

            if (!capable) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "do not support storage migration of volume[uuid:%s] from [%s] to [%s]",
                        volumeVO.getUuid(), srcPsType, dstPsType));
            }
        }

        validateVmStorageLayout(vmInstanceVO, specs, msg.getDstHostUuid(), msg.getSystemTags(), legacyRootOnly);
    }

    private void validate(APIBackupStorageMigrateImageMsg msg) {
        BackupStorageVO srcBS = dbf.findByUuid(msg.getSrcBackupStorageUuid(), BackupStorageVO.class);
        BackupStorageVO dstBS = dbf.findByUuid(msg.getDstBackupStorageUuid(), BackupStorageVO.class);
        // srcBS != dstBS
        if (srcBS.getUuid().equals(dstBS.getUuid())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Source BS and Destination BS cannot be the same."
            ));
        }

        // srcBS and dstBS are not Disabled
        if (srcBS.getState() == BackupStorageState.Disabled || dstBS.getState() == BackupStorageState.Disabled) {
            throw new ApiMessageInterceptionException(
                    Platform.argerr("Source BS and Destination BS must not be Disabled.")
            );
        }

        // image is ready
        ImageVO image = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        if (image.getStatus() != ImageStatus.Ready) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Image[uuid:%s] is not in status Ready, cannot migrate it.", msg.getImageUuid()
            ));
        }

        // image is in srcBS
        ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getSrcBackupStorageUuid())
                .find();
        if (ref == null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Image[uuid:%s] is not in source backup storage[uuid:%s]", msg.getImageUuid(), msg.getSrcBackupStorageUuid()
            ));
        }

        String srcBsType = srcBS.getType();
        String dstBsType = dstBS.getType();
        List<String> types = backupStorageBackupStorageMetrics.get(srcBsType);
        if (types == null || !types.contains(dstBsType)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Cannot migrate image from %s to %s.", srcBS.getType(), dstBS.getType())
            );
        }

        // Ceph <-> Ceph
        if (srcBsType.equalsIgnoreCase(CephConstants.CEPH_BACKUP_STORAGE_TYPE)) {
            msg.setType(StorageMigrationConstant.CEPH_TO_CEPH_MIGRATE_IMAGE_TYPE);
        }
    }

    private void validate(APIPrimaryStorageMigrateVolumeMsg msg) {
        VolumeVO srcVolume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        VmInstanceVO attachedVm = null;
        msg.setSrcPrimaryStorageUuid(srcVolume.getPrimaryStorageUuid());
        PrimaryStorageVO srcPS = dbf.findByUuid(msg.getSrcPrimaryStorageUuid(), PrimaryStorageVO.class);
        PrimaryStorageVO dstPS = dbf.findByUuid(msg.getDstPrimaryStorageUuid(), PrimaryStorageVO.class);

        // srcPsUuid != dstPsUuid
        if (msg.getSrcPrimaryStorageUuid().equals(msg.getDstPrimaryStorageUuid()) && !srcPS.getType().equals(CephConstants.CEPH_PRIMARY_STORAGE_TYPE)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Volume[uuid:%s] is already in PS[uuid:%s], cannot migrate.", msg.getVolumeUuid(), msg.getDstPrimaryStorageUuid()
            ));
        }

        // srcPS and dstPS not disabled or maintenance
        if (srcPS.getState() == PrimaryStorageState.Disabled || srcPS.getState() == PrimaryStorageState.Maintenance ||
                dstPS.getState() == PrimaryStorageState.Disabled || dstPS.getState() == PrimaryStorageState.Maintenance) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Source PS and Destination PS must not be Disabled or Maintenance state."
            ));
        }

        // volume is ready
        if (srcVolume.getStatus() != VolumeStatus.Ready) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Volume[uuid:%s] is not in status Ready, cannot migrate it.", msg.getVolumeUuid()
            ));
        }

        if (srcVolume.getType() == VolumeType.Root) {
            VmInstanceVO srcVm = dbf.findByUuid(srcVolume.getVmInstanceUuid(), VmInstanceVO.class);
            msg.setVmInstanceUuid(srcVm.getUuid());

            checkAttachedISO(srcVm);

            if (srcVm.getState() != VmInstanceState.Stopped) {
                throw new ApiMessageInterceptionException(
                        Platform.argerr("Cannot migrate root volume when vm instance is not stopped.")
                );
            }

            if (Q.New(ShareableVolumeVmInstanceRefVO.class).eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, srcVm.getUuid()).isExists()) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "do not support storage migration of vm[uuid:%s, name: %s] while shared volume attached", srcVm.getUuid(), srcVm.getName()
                ));
            }

            for (VolumeVO vol : srcVm.getAllDiskVolumes()) {
                if (srcPS.getType().equals(StorageMigrationConstant.SHAREDBLOCK_STORAGE_TYPE)) {
                    continue;
                }

                if (vol.getType() == VolumeType.Data) {
                    throw new ApiMessageInterceptionException(
                            Platform.argerr("Cannot migrate root volume when there are data volumes attached to the vm instance.")
                    );
                }
            }

            // get proper destination cluster: must be a single cluster reachable by ALL VM NICs
            Set<String> dstClusterUuids = dstPS.getAttachedClusterRefs().stream()
                    .map(PrimaryStorageClusterRefVO::getClusterUuid)
                    .collect(Collectors.toSet());
            Set<String> reachableClusterUuids = srcVm.getVmNics().isEmpty()
                    ? new HashSet<>(dstClusterUuids) : null;
            for (VmNicVO vmNic : srcVm.getVmNics()) {
                L3NetworkVO l3 = dbf.findByUuid(vmNic.getL3NetworkUuid(), L3NetworkVO.class);
                L2NetworkVO l2 = dbf.findByUuid(l3.getL2NetworkUuid(), L2NetworkVO.class);
                Set<String> nicClusterUuids = new HashSet<>(Q.New(L2NetworkClusterRefVO.class)
                        .select(L2NetworkClusterRefVO_.clusterUuid)
                        .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2.getUuid())
                        .in(L2NetworkClusterRefVO_.clusterUuid, dstClusterUuids)
                        .listValues());
                if (reachableClusterUuids == null) {
                    reachableClusterUuids = nicClusterUuids;
                } else {
                    reachableClusterUuids.retainAll(nicClusterUuids);
                }
                if (reachableClusterUuids.isEmpty()) {
                    break;
                }
            }
            if (reachableClusterUuids == null || reachableClusterUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(
                        Platform.argerr("The destination primary storage is not attached to any cluster that has the same L2 networks as source cluster.")
                );
            }
        } else if (srcVolume.getType() == VolumeType.Data) {
            if (srcVolume.getVmInstanceUuid() != null) {
                if (!srcPS.getType().equals(StorageMigrationConstant.SHAREDBLOCK_STORAGE_TYPE)) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "the volume[uuid:%s] is still attached on vm[uuid:%s], please detach it before migration.",
                            msg.getVolumeUuid(), srcVolume.getVmInstanceUuid()
                    ));
                }

                attachedVm = dbf.findByUuid(srcVolume.getVmInstanceUuid(), VmInstanceVO.class);
                if (attachedVm.getState() != VmInstanceState.Stopped) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "cannot migrate data volume[uuid:%s] bewteen sharedblock primary storages when vm[vmuuid:%s] instance is not stopped.",
                            msg.getVolumeUuid(), srcVolume.getVmInstanceUuid()
                    ));
                }
            }

            if (Q.New(ShareableVolumeVmInstanceRefVO.class).eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, srcVolume.getUuid()).isExists()) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "do not support storage migration while shared volume[uuid: %s, name: %s] attached", srcVolume.getUuid(), srcVolume.getName()
                ));
            }
        }

        String srcPsType = srcPS.getType();
        String dstPsType = dstPS.getType();
        List<String> types = primaryStoragePrimaryStorageMetrics.get(srcPsType);
        if (types == null || !types.contains(dstPsType)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "Cannot migrate volume from %s to %s.", srcPS.getType(), dstPS.getType())
            );
        }

        if (attachedVm != null) {
            String invalidReason = StorageMigrationLayoutChecker.validateFinalLayout(
                    attachedVm, Collections.singletonMap(srcVolume.getUuid(), dstPS.getUuid()),
                    null, msg.getSystemTags());
            if (invalidReason != null) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "cannot migrate volume[uuid:%s] because %s", srcVolume.getUuid(), invalidReason));
            }
        }

        // Ceph <-> Ceph
        if (srcPsType.equalsIgnoreCase(CephConstants.CEPH_PRIMARY_STORAGE_TYPE)) {
            msg.setType(StorageMigrationConstant.CEPH_TO_CEPH_MIGRATE_VOLUME_TYPE);
        }

        // NFS <-> NFS
        if (srcPsType.equalsIgnoreCase(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)) {
            msg.setType(StorageMigrationConstant.NFS_TO_NFS_MIGRATE_VOLUME_TYPE);
        }

        // SharedBlock <-> SharedBlock
        if (srcPsType.equalsIgnoreCase(StorageMigrationConstant.SHAREDBLOCK_STORAGE_TYPE)) {
            msg.setType(StorageMigrationConstant.SHAREDBLOCK_TO_SHAREDBLOCK_MIGRATE_VOLUME_TYPE);
        }

        if (srcVolume.getState().equals(VolumeState.Disabled)) {
            throw new ApiMessageInterceptionException(Platform.argerr("can not migrate volume[%s], " +
                    "because volume state is Disabled", msg.getVolumeUuid()));
        }
    }
}
