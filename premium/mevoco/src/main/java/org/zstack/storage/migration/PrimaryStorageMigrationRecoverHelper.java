package org.zstack.storage.migration;

import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.primary.PrimaryStorageLiveMigrateVmMsg;
import org.zstack.storage.migration.primary.PrimaryStorageMigrateVmMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;

public class PrimaryStorageMigrationRecoverHelper {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageMigrationRecoverHelper.class);

    private static final List<VmInstanceState> allowRecoveryVmCurrentState = Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused, VmInstanceState.Migrating);
    private static final List<VmInstanceState> allowRecoveryVmOriginState = Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused);

    public static boolean needRecoverStorageMigration(PrimaryStorageMigrateVmContext ctx, String vmUuid) {
        if (!ctx.isMigrateStarted()) {
            return false;
        }
        if (!allowRecoveryVmOriginState.contains(VmInstanceState.valueOf(ctx.getVmOriginState()))) {
            return false;
        }
        VmInstanceState currentState = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).select(VmInstanceVO_.state).findValue();
        if (!allowRecoveryVmCurrentState.contains(currentState)) {
            return false;
        }
        return ctx.getCreatedVolumes() != null && ctx.getInitializedVolumes() != null && ctx.getCreatedVolumes().size() == ctx.getInitializedVolumes().size();
    }

    public static VmInstanceState saveAndGetVmStatus(PrimaryStorageMigrateVmMsg msg) {
        VmInstanceState currentState = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).select(VmInstanceVO_.state).findValue();
        if (!msg.getJobContext().isMigrateStarted()) {
            msg.getJobContext().setVmOriginState(currentState.toString());
            LongJobContextUtil.saveContext(msg.getLongJobUuid(), msg.getJobContext());
            return currentState;
        }

        return VmInstanceState.valueOf(msg.getJobContext().getVmOriginState());
    }

    public static List<VolumeVO> getVolumesToMigrateFromJobContext(PrimaryStorageLiveMigrateVmMsg msg, List<VolumeVO> allVolumes, List<VolumeVO> migrateVolumes) {
        if (!msg.getJobContext().isMigrateStarted()) {
            return migrateVolumes;
        }

        for (VolumeVO vol : allVolumes) {
            if (msg.getJobContext().getVolumeMappingDict().containsKey(vol.getUuid())) {
                migrateVolumes.remove(vol);
                migrateVolumes.add(0, vol);
                logger.debug(String.format("the last migration of volume[uuid:%s] was interrupted, now to migrate it first, " +
                                "the dst primary storage is [%s] and the dst volume path is [%s]" , vol.getUuid(), msg.getDstPrimaryStorageUuid(), getDstVolumeFromJobContext(msg.getJobContext(), vol.getUuid()).getInstallPath()));
            }
        }

        return migrateVolumes;
    }

    public static VolumeInventory getDstVolumeFromJobContext(PrimaryStorageMigrateVmContext ctx, String srcVolumeUuid) {
        String newVolumeUuid = ctx.getVolumeMappingDict().get(srcVolumeUuid);
        VolumeInventory dstVolume = ctx.getInitializedVolumes().stream().filter(vol -> vol.getUuid().equals(newVolumeUuid)).findFirst().orElse(null);
        if (dstVolume == null) {
            throw new CloudRuntimeException("cannot find dst volume to recover stoarge migration");
        }
        return dstVolume;
    }
}
