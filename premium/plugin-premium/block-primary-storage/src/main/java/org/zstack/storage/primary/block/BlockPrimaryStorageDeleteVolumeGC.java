package org.zstack.storage.primary.block;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.primary.block.message.DeleteVolumeLunOnPrimaryStorageMsg;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/7/18 22:09
 */
public class BlockPrimaryStorageDeleteVolumeGC extends TimeBasedGarbageCollector {
    @GC
    public String primaryStorageUuid;
    @GC
    public VolumeInventory volume;
    @GC
    public BlockScsiLunVO blockScsiLunVO;

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class)) {
            completion.cancel();
            return;
        }

        DeleteVolumeLunOnPrimaryStorageMsg msg = new DeleteVolumeLunOnPrimaryStorageMsg();
        msg.setBitsUuid(volume.getUuid());
        msg.setLunName(blockScsiLunVO.getName());
        msg.setLunId(blockScsiLunVO.getId());
        msg.setBitsType(BlockScsiLunVO.class.getSimpleName());
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, BlockPrimaryStorageConstants.SERVICE_ID, primaryStorageUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }
}
