package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.DebugUtils;

import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.findOneOrNull;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmImageSelectBackupStorageFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public String name() {
        return "choose-backup-storage-for-downloading-the-image";
    }

    private String findBackupStorage(VmInstanceSpec spec, String imageUuid) {
        spec.getImageSpec().setNeedDownload(imageNeedDownload(spec, imageUuid));
        if (!spec.getImageSpec().isNeedDownload() && spec.getImageSpec().getInventory().getBackupStorageRefs().isEmpty()) {
            return null;
        }

        if (spec.getImageSpec().getInventory().getBackupStorageRefs().size() == 1) {
            return spec.getImageSpec().getInventory().getBackupStorageRefs().iterator().next().getBackupStorageUuid();
        }

        DebugUtils.Assert(spec.getVmInventory().getZoneUuid() != null, String.format("image[uuid:%s] is on multiple backup storages, " +
                "zoneUuid must be set to select backup storage for downloading the image", imageUuid));

        ImageBackupStorageSelector selector = new ImageBackupStorageSelector();
        selector.setZoneUuid(spec.getVmInventory().getZoneUuid());
        selector.setImageUuid(imageUuid);
        String bsUuid = selector.select();

        if (bsUuid != null) {
            return bsUuid;
        }

        if (!spec.getImageSpec().isNeedDownload()) {
            // the image is already on the primary storage,
            // in this case, the backup storage needs not to be Connected
            selector.setCheckStatus(false);
            bsUuid = selector.select();
            if (bsUuid != null) {
                return bsUuid;
            }
        }

        if (spec.getVmInventory().getZoneUuid() != null) {
            throw new OperationFailureException(operr("cannot find the image[uuid:%s] in any connected backup storage attached to the zone[uuid:%s]. check below:\n" +
                                    "1. if the backup storage is attached to the zone where the VM[name: %s, uuid:%s] is in\n" +
                                    "2. if the backup storage is in connected status, if not, try reconnecting it",
                            imageUuid, spec.getVmInventory().getZoneUuid(), spec.getVmInventory().getName(), spec.getVmInventory().getUuid())
            );
        } else {
            throw new OperationFailureException(operr("cannot find the image[uuid:%s] in any connected backup storage. check below:\n" +
                                    "1. if the backup storage is attached to the zone where the VM[name: %s, uuid:%s] is in\n" +
                                    "2. if the backup storage is in connected status, if not, try reconnecting it",
                            imageUuid, spec.getVmInventory().getName(), spec.getVmInventory().getUuid())
            );
        }
    }

    private boolean imageNeedDownload(VmInstanceSpec spec, String imageUuid) {
        List<String> psUuid;
        if (VmOperation.NewCreate == spec.getCurrentVmOperation()) {
            psUuid = spec.getVolumeSpecs().isEmpty() ? spec.getCandidatePrimaryStorageUuidsForRootVolume() :
                    Collections.singletonList(spec.getVolumeSpecs().get(0).getPrimaryStorageInventory().getUuid());
        } else {
            psUuid = Collections.singletonList(spec.getVmInventory().getRootVolume().getPrimaryStorageUuid());
        }

        if (psUuid.isEmpty()) {
            return true;
        }

        List<String> hasImageCachePsUuids = Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, imageUuid)
                .in(ImageCacheVO_.primaryStorageUuid, psUuid)
                .select(ImageCacheVO_.primaryStorageUuid)
                .listValues();

        return new HashSet<>(hasImageCachePsUuids).size() < psUuid.size();
    }

    @Transactional(readOnly = true)
    private String findIsoBsUuidInTheZone(final String isoImageUuid, final String zoneUuid) {
        String sql = "select ref.backupStorageUuid" +
                " from ImageBackupStorageRefVO ref, BackupStorageZoneRefVO zoneref" +
                " where ref.backupStorageUuid = zoneref.backupStorageUuid" +
                " and zoneref.zoneUuid = :zoneUuid" +
                " and ref.imageUuid = :imgUuid";

        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("zoneUuid", zoneUuid);
        q.setParameter("imgUuid", isoImageUuid);
        q.setMaxResults(1);
        List<String> ret = q.getResultList();
        if (ret.isEmpty()) {
            throw new OperationFailureException(operr("no backup storage attached to the zone[uuid:%s] contains the ISO[uuid:%s]",
                            zoneUuid, isoImageUuid));
        }

        return ret.get(0);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final VmOperation operation = spec.getCurrentVmOperation();

        if (VmOperation.NewCreate == operation || VmOperation.ChangeImage == operation) {
            runWithNewCreateOrChangeImageOperation(spec);
        } else if (VmOperation.Start == operation || VmOperation.Reboot == operation) {
            runWithStartOrRebootOperation(spec);
        }

        trigger.next();
    }

    private void runWithNewCreateOrChangeImageOperation(VmInstanceSpec spec) {
        final VmInstanceSpec.ImageSpec imageSpec = spec.getImageSpec();
        if (imageSpec.getInventory() != null) {
            final ImageInventory image = imageSpec.getInventory();
            final String bsUuid = findBackupStorage(spec, image.getUuid());

            imageSpec.setSelectedBackupStorage(findOneOrNull(
                    image.getBackupStorageRefs(),
                    arg -> arg.getBackupStorageUuid().equals(bsUuid) && ImageStatus.Ready.toString().equals(arg.getStatus())));

            if (ImageMediaType.ISO.toString().equals(image.getMediaType())) {
                VmInstanceSpec.CdRomSpec cdRomSpec = findOneOrNull(
                        spec.getCdRomSpecs(),
                        cdromSpec -> Objects.equals(cdromSpec.getImageUuid(), image.getUuid()));
                if (cdRomSpec != null) {
                    cdRomSpec.setBackupStorageUuid(bsUuid);
                }
            }
        }

        for (VmInstanceSpec.CdRomSpec cdRomSpec : spec.getCdRomSpecs()) {
            if (cdRomSpec.getBackupStorageUuid() != null) {
                continue;
            }
            if (cdRomSpec.getImageUuid() == null) {
                continue;
            }
            cdRomSpec.setBackupStorageUuid(
                    findIsoBsUuidInTheZone(cdRomSpec.getImageUuid(), spec.getVmInventory().getZoneUuid()));
        }
    }

    private void runWithStartOrRebootOperation(VmInstanceSpec spec) {
        if (spec.getCdRomSpecs().isEmpty()) {
            return;
        }

        for (VmInstanceSpec.CdRomSpec cdRomSpec : spec.getCdRomSpecs()) {
            if (cdRomSpec.getImageUuid() == null) {
                continue;
            }
            cdRomSpec.setBackupStorageUuid(
                    findIsoBsUuidInTheZone(cdRomSpec.getImageUuid(), spec.getVmInventory().getZoneUuid()));
        }
    }
}
