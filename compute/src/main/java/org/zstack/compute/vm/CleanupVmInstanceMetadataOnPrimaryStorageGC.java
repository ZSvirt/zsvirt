package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.CleanupVmInstanceMetadataOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.metadata.VmMetadataPathBuildExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class CleanupVmInstanceMetadataOnPrimaryStorageGC extends TimeBasedGarbageCollector {
    private static final CLogger logger = Utils.getLogger(CleanupVmInstanceMetadataOnPrimaryStorageGC.class);

    @Autowired
    private PluginRegistry pluginRgty;

    @GC
    public String primaryStorageUuid;
    @GC
    public String vmUuid;
    @GC
    public String rootVolumeUuid;
    @GC
    public String metadataPath;
    @GC
    public String hostUuid;

    public static String getGCName(String vmUuid) {
        return String.format("gc-cleanup-vm-metadata-%s", vmUuid);
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class)) {
            logger.debug(String.format("[MetadataCleanupGC] primary storage[uuid:%s] no longer exists, " +
                    "cancel gc for vm[uuid:%s]", primaryStorageUuid, vmUuid));
            completion.cancel();
            return;
        }

        String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).eq(PrimaryStorageVO_.uuid, primaryStorageUuid).findValue();
        if (psType == null) {
            logger.debug(String.format("[MetadataCleanupGC] primary storage[uuid:%s] type not found, " +
                    "cancel gc for vm[uuid:%s]", primaryStorageUuid, vmUuid));
            completion.cancel();
            return;
        }

        VmMetadataPathBuildExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, VmMetadataPathBuildExtensionPoint.class);
        if (ext == null) {
            logger.debug(String.format("[MetadataCleanupGC] ps[uuid:%s, type:%s] does not support metadata cleanup, " +
                    "cancel gc for vm[uuid:%s]", primaryStorageUuid, psType, vmUuid));
            completion.cancel();
            return;
        }

        boolean requireHost = ext.requireHostForCleanup();

        // Determine effective hostUuid based on whether the PS type requires a host for cleanup.
        String effectiveHostUuid = hostUuid;
        if (!requireHost) {
            effectiveHostUuid = null;
        } else {
            if (effectiveHostUuid == null && rootVolumeUuid == null) {
                logger.debug(String.format("[MetadataCleanupGC] hostUuid and rootVolumeUuid are both null, " +
                                "ps[uuid:%s, type:%s] requires host for cleanup, cancel gc for vm[uuid:%s]",
                        primaryStorageUuid, psType, vmUuid));
                completion.cancel();
                return;
            }
            if (effectiveHostUuid != null && !dbf.isExist(effectiveHostUuid, HostVO.class)) {
                logger.debug(String.format("[MetadataCleanupGC] host[uuid:%s] no longer exists " +
                                "and ps[uuid:%s, type:%s] requires host for cleanup, " +
                                "metadata is unreachable, cancel gc for vm[uuid:%s]",
                        effectiveHostUuid, primaryStorageUuid, psType, vmUuid));
                completion.cancel();
                return;
            }
        }

        CleanupVmInstanceMetadataOnPrimaryStorageMsg msg = new CleanupVmInstanceMetadataOnPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        msg.setVmInstanceUuid(vmUuid);
        msg.setRootVolumeUuid(rootVolumeUuid);
        msg.setMetadataPath(metadataPath);
        msg.setHostUuid(effectiveHostUuid);

        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.info(String.format("[MetadataCleanupGC] successfully cleaned up metadata " +
                            "for vm[uuid:%s] on ps[uuid:%s]", vmUuid, primaryStorageUuid));
                    completion.success();
                } else {
                    logger.warn(String.format("[MetadataCleanupGC] failed to clean up metadata " +
                            "for vm[uuid:%s] on ps[uuid:%s]: %s", vmUuid, primaryStorageUuid, reply.getError()));
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
