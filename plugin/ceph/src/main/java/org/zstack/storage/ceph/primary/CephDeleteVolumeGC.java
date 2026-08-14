package org.zstack.storage.ceph.primary;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.volume.VolumeErrors;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by kayo on 2018/7/24.
 */
public class CephDeleteVolumeGC extends TimeBasedGarbageCollector {
    @GC
    public String primaryStorageUuid;
    @GC
    public VolumeInventory volume;

    private static final CLogger logger = Utils.getLogger(CephDeleteVolumeGC.class);

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class)) {
            completion.cancel();
            return;
        }

        DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        msg.setInstallPath(volume.getInstallPath());
        msg.setSize(volume.getSize());
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    if (reply.getError().isError(VolumeErrors.VOLUME_IN_USE)) {
                        logger.warn(String.format("unable to delete path:%s right now, cancel this GC job because it's in use", msg.getInstallPath()));
                        completion.cancel();
                        return;
                    }
                    completion.fail(reply.getError());
                    return;
                }

                completion.success();
            }
        });
    }
}
