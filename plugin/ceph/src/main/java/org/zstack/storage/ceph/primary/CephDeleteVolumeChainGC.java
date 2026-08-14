package org.zstack.storage.ceph.primary;

import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeChainOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.DeleteVolumeChainOnPrimaryStorageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class CephDeleteVolumeChainGC extends TimeBasedGarbageCollector {
    @GC
    public String primaryStorageUuid;
    @GC
    public List<String> installPaths;
    @GC
    public String chainTop;

    private static final CLogger logger = Utils.getLogger(CephDeleteVolumeChainGC.class);

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class) || CollectionUtils.isEmpty(installPaths)) {
            completion.cancel();
            return;
        }

        DeleteVolumeChainOnPrimaryStorageMsg msg = new DeleteVolumeChainOnPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        msg.setChainTop(chainTop);
        msg.setInstallPaths(installPaths);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                DeleteVolumeChainOnPrimaryStorageReply r = reply.castReply();
                if (CollectionUtils.isEmpty(r.getUndeletedInstallPaths())) {
                    completion.success();
                } else {
                    installPaths = r.getUndeletedInstallPaths();
                    updateContext();
                    completion.fail(Platform.operr("delete volume chain error, continue to delete"));
                }
            }
        });
    }
}

