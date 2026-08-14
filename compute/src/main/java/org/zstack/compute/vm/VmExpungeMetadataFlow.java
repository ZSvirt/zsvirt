package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.CleanupVmInstanceMetadataOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.metadata.VmMetadataPathBuildExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmExpungeMetadataFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmExpungeMetadataFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
            trigger.next();
            return;
        }

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec == null || spec.getVmInventory() == null) {
            logger.warn("[MetadataExpunge] missing VmInstanceSpec or VmInventory, skip metadata cleanup");
            trigger.next();
            return;
        }

        final String vmUuid = spec.getVmInventory().getUuid();

        VolumeInventory rootVolume = spec.getVmInventory().getRootVolume();
        String psUuid = rootVolume != null ? rootVolume.getPrimaryStorageUuid() : null;
        if (psUuid == null) {
            logger.debug(String.format("[MetadataExpunge] vm[uuid:%s] root volume has no primaryStorageUuid, " +
                    "skipping metadata cleanup", vmUuid));
            trigger.next();
            return;
        }


        String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).eq(PrimaryStorageVO_.uuid, psUuid).findValue();
        if (psType == null) {
            logger.warn(String.format("[MetadataExpunge] primary storage[uuid:%s] not found for vm[uuid:%s], " +
                    "skip metadata cleanup", psUuid, vmUuid));
            trigger.next();
            return;
        }

        VmMetadataPathBuildExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, VmMetadataPathBuildExtensionPoint.class);
        if (ext == null) {
            logger.warn(String.format("[MetadataExpunge] no VmMetadataPathBuildExtensionPoint found for ps[uuid:%s, type:%s], " +
                    "skip metadata cleanup", psUuid, psType));
            trigger.next();
            return;
        }
        final String metadataPath;
        try {
            metadataPath = ext.buildVmMetadataPath(psUuid, vmUuid);
        } catch (Exception e) {
            logger.warn(String.format("[MetadataExpunge] failed to build metadata path for vm[uuid:%s] on ps[uuid:%s], " +
                    "skip metadata cleanup: %s", vmUuid, psUuid, e.getMessage()));
            trigger.next();
            return;
        }

        String hostUuid = null;
        if (ext.requireHostForCleanup()) {
            hostUuid = spec.getVmInventory().getHostUuid();
            if (hostUuid == null) {
                hostUuid = spec.getVmInventory().getLastHostUuid();
            }

            if (hostUuid == null) {
                logger.warn(String.format("[MetadataExpunge] vm[uuid:%s] hostUuid is null, " +
                                "ps[uuid:%s, type:%s] requires host for cleanup, skip without submitting GC",
                        vmUuid, psUuid, psType));
                trigger.next();
                return;
            }
        }

        String rootVolumeUuid = rootVolume.getUuid();
        CleanupVmInstanceMetadataOnPrimaryStorageMsg cmsg = new CleanupVmInstanceMetadataOnPrimaryStorageMsg();
        cmsg.setPrimaryStorageUuid(psUuid);
        cmsg.setVmInstanceUuid(vmUuid);
        cmsg.setMetadataPath(metadataPath);
        cmsg.setRootVolumeUuid(rootVolumeUuid);
        cmsg.setHostUuid(hostUuid);
        final String finalPsUuid = psUuid;
        final String finalHostUuid = hostUuid;

        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(cmsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.info(String.format("[MetadataExpunge] successfully deleted metadata for vm[uuid:%s] on ps[uuid:%s]",
                            vmUuid, finalPsUuid));
                } else {
                    logger.warn(String.format("[MetadataExpunge] failed to delete metadata for vm[uuid:%s] on ps[uuid:%s]: %s, " +
                            "submitting GC job for retry", vmUuid, finalPsUuid, reply.getError()));
                    submitGC(finalPsUuid, vmUuid, rootVolumeUuid, metadataPath, finalHostUuid);
                }
                trigger.next();
            }
        });
    }

    private void submitGC(String psUuid, String vmUuid, String rootVolumeUuid, String metadataPath, String hostUuid) {
        CleanupVmInstanceMetadataOnPrimaryStorageGC gc = new CleanupVmInstanceMetadataOnPrimaryStorageGC();
        gc.NAME = CleanupVmInstanceMetadataOnPrimaryStorageGC.getGCName(vmUuid);
        gc.primaryStorageUuid = psUuid;
        gc.vmUuid = vmUuid;
        gc.rootVolumeUuid = rootVolumeUuid;
        gc.metadataPath = metadataPath;
        gc.hostUuid = hostUuid;
        long gcIntervalSec = TimeUnit.HOURS.toSeconds(VmGlobalConfig.VM_METADATA_CLEANUP_GC_INTERVAL.value(Long.class));
        gc.deduplicateSubmit(gcIntervalSec, TimeUnit.SECONDS);

        logger.info(String.format("[MetadataExpunge] submitted GC job [%s] for vm[uuid:%s] on ps[uuid:%s]", gc.NAME, vmUuid, psUuid));
    }
}
