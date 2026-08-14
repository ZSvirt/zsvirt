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
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

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
            validate((APIPrimaryStorageMigrateVmMsg) msg);
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

    private void validate(APIPrimaryStorageMigrateVmMsg msg) {
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        Set<String> canMigrateVolumeVOS = new HashSet<>();

        checkAttachedISO(vmInstanceVO);

        List<VmStorageMigrationMetric> vmStorageMigrationMetrics =
                new ArrayList<>(pluginRegistry.getExtensionList(VmStorageMigrationMetric.class));

        PrimaryStorageVO dstPrimaryStorageVO = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, msg.getDstPrimaryStorageUuid())
                .find();

        if (Q.New(ShareableVolumeVmInstanceRefVO.class).eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vmInstanceVO.getUuid()).isExists()) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "do not support storage migration of vm[uuid:%s, name: %s] while shared volume attached", vmInstanceVO.getUuid(), vmInstanceVO.getName()
            ));
        }


        for (VolumeVO volumeVO : vmInstanceVO.getAllDiskVolumes()) {
            if (!msg.isWithDataVolumes() && volumeVO.getType() == VolumeType.Data) {
                continue;
            }

            PrimaryStorageVO primaryStorageVO = Q.New(PrimaryStorageVO.class)
                    .eq(PrimaryStorageVO_.uuid, volumeVO.getPrimaryStorageUuid())
                    .find();

            String srcPsType = primaryStorageVO.getType();
            String dstPsType = dstPrimaryStorageVO.getType();
            for (VmStorageMigrationMetric metric : vmStorageMigrationMetrics) {
                if (!metric.isCapable(srcPsType, dstPsType)) {
                    continue;
                }

                if ((vmInstanceVO.getState().equals(VmInstanceState.Running) || vmInstanceVO.getState().equals(VmInstanceState.Paused)) &&
                        !metric.isSupportLive()) {
                    continue;
                }

                if (vmInstanceVO.getState().equals(VmInstanceState.Stopped) &&
                        !metric.isSupportOffline()) {
                    continue;
                }

                if (msg.isWithDataVolumes() && !metric.isSupportWithDataVolume()) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "do not support storage migration from [%s] to [%s] with data volume", srcPsType, dstPsType));
                }

                if (msg.isWithSnapshots() && !metric.isSupportWithSnapshot()) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "do not support storage migration from [%s] to [%s] with snapshot", srcPsType, dstPsType));
                }
                canMigrateVolumeVOS.add(volumeVO.getUuid());
            }

            if (!canMigrateVolumeVOS.contains(volumeVO.getUuid())) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "do not support storage migration from [%s] to [%s]", primaryStorageVO.getType(), dstPrimaryStorageVO.getType()));
            }

            if (!vmInstanceVO.getState().equals(VmInstanceState.Stopped) &&
                    vmInstanceVO.getHostUuid() == null) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "VM[uuid:%s] is running but host uuid is missing", vmInstanceVO.getUuid()));
            }
        }
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

            // get proper destination cluster
            for (VmNicVO vmNic : srcVm.getVmNics()) {
                boolean match = false;
                L3NetworkVO l3 = dbf.findByUuid(vmNic.getL3NetworkUuid(), L3NetworkVO.class);
                L2NetworkVO l2 = dbf.findByUuid(l3.getL2NetworkUuid(), L2NetworkVO.class);
                for (PrimaryStorageClusterRefVO pcRef : dstPS.getAttachedClusterRefs()) {
                    match = Q.New(L2NetworkClusterRefVO.class)
                            .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2.getUuid())
                            .eq(L2NetworkClusterRefVO_.clusterUuid, pcRef.getClusterUuid())
                            .isExists();
                    if (match) break;
                }
                if (!match) {
                    throw new ApiMessageInterceptionException(
                            Platform.argerr("The destination primary storage is not attached to any cluster that has the same L2 networks as source cluster.")
                    );
                }
            }
        } else if (srcVolume.getType() == VolumeType.Data) {
            if (srcVolume.getVmInstanceUuid() != null) {
                VmInstanceState state = Q.New(VmInstanceVO.class).select(VmInstanceVO_.state).eq(VmInstanceVO_.uuid, srcVolume.getVmInstanceUuid()).findValue();
                if (!srcPS.getType().equals(StorageMigrationConstant.SHAREDBLOCK_STORAGE_TYPE)) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "the volume[uuid:%s] is still attached on vm[uuid:%s], please detach it before migration.",
                            msg.getVolumeUuid(), srcVolume.getVmInstanceUuid()
                    ));
                } else if (state != VmInstanceState.Stopped) {
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
