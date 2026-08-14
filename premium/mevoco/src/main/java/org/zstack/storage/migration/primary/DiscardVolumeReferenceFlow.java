package org.zstack.storage.migration.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.StorageMigrationConstant;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DiscardVolumeReferenceFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(DiscardVolumeReferenceFlow.class);

    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected CloudBus bus;

    @Override
    public boolean skip(Map data) {
        boolean discardSource = (boolean) data.get(StorageMigrationConstant.DISCARD_SOURCE);
        return !discardSource;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VolumeInventory volume = (VolumeInventory) data.get(StorageMigrationConstant.SRC_VOLUME);
        List<VolumeSnapshotInventory> snapshots = (List<VolumeSnapshotInventory>) data.get(StorageMigrationConstant.SRC_VOLUME_SNAPSHOTS);
        List<Object> objs = getVolumeResourcesToTrash(volume, snapshots);
        data.put(StorageMigrationConstant.OBJECTS_TO_TRASH, objs);
        VolumeSnapshotReferenceUtils.handleVolumeDeletion(volume);
        trigger.next();
    }

    /**
     * Get the volume and its snapshots to be trashed.
     * If the volume is referenced by other volumes, it will not be trashed.
     * If the snapshots are referenced by other volumes, they will not be trashed.
     *
     * @param vol       the volume to be trashed
     * @param snapshots the snapshots of the volume
     * @return a list containing the volume and its snapshots to be trashed
     */

    public static List<Object> getVolumeResourcesToTrash(VolumeInventory vol, List<VolumeSnapshotInventory> snapshots) {
        AskVolumeSnapshotCapabilityMsg amsg = new AskVolumeSnapshotCapabilityMsg();
        amsg.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
        amsg.setVolume(vol);
        CloudBus bus = Platform.getComponentLoader().getComponent(CloudBus.class);
        bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
        MessageReply areply = bus.call(amsg);
        if (!areply.isSuccess()) {
            throw new OperationFailureException(operr(areply.getError().toString()));
        }

        AskVolumeSnapshotCapabilityReply acap = areply.castReply();

        boolean innerSnapshot = acap.getCapability().getVolumePathFromInnerSnapshotRegex() != null;
        if (innerSnapshot) {
            Pattern p = Pattern.compile(acap.getCapability().getVolumePathFromInnerSnapshotRegex());
            return getInnerVolumeResourcesToTrash(vol, snapshots == null ? Collections.emptyList() : snapshots,
                    s -> {
                        Matcher m = p.matcher(s);
                        if (m.find()) {
                            return m.group();
                        } else {
                            throw new OperationFailureException(operr(
                                    "can not find volume path from snapshot install path[%s] by regex[%s]",
                                    s, p.pattern()));
                        }
                    });
        } else {
            return getChainVolumeResourcesToTrash(vol, snapshots == null ? Collections.emptyList() : snapshots);
        }
    }

    private static List<Object> getChainVolumeResourcesToTrash(VolumeInventory volInv, List<VolumeSnapshotInventory> snapshotInvs) {
        List<Object> objs = new ArrayList<>();
        objs.add(volInv);

        Set<String> refSnapshotUuids = VolumeSnapshotReferenceUtils.getVolumeAllSnapshotsReferencedByOtherVolumes(volInv.getUuid());
        if (refSnapshotUuids.isEmpty()) {
            objs.addAll(snapshotInvs);
            return objs;
        }

        logger.debug(String.format("other volume is backing on us[volumeSnapshotUuids:%s], skip discard source", refSnapshotUuids));
        for (VolumeSnapshotInventory snapshot : snapshotInvs) {
            if (!refSnapshotUuids.contains(snapshot.getUuid())) {
                objs.add(snapshot);
            }
        }
        return objs;
    }

    private static List<Object> getInnerVolumeResourcesToTrash(VolumeInventory volInv, List<VolumeSnapshotInventory> snapshotInvs, Function<String, String> volPathExtractor) {
        List<Object> objs = new ArrayList<>();

        // Fixes ZSTAC-20943: new snapshot tree will be created when ReimageVmInstance is called
        Set<String> volPaths = snapshotInvs.stream()
                .map(it -> volPathExtractor.apply(it.getPrimaryStorageInstallPath()))
                .collect(Collectors.toSet());
        volPaths.add(volInv.getInstallPath());

        for (String path : volPaths) {
            VolumeInventory vol = JSONObjectUtil.rehashObject(volInv, VolumeInventory.class);
            vol.setInstallPath(path);
            if (VolumeSnapshotReferenceUtils.isVolumeDirectlyReferenceByOthers(volInv)) {
                logger.debug(String.format("other volume is backing on us[volumeUuid:%s, path:%s], skip discard source",
                        vol.getUuid(), path));
            } else {
                objs.add(vol);
            }
        }
        return objs;
    }
}
