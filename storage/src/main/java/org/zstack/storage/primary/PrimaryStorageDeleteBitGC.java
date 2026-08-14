package org.zstack.storage.primary;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/1/12.
 */
public class PrimaryStorageDeleteBitGC extends TimeBasedGarbageCollector {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageDeleteBitGC.class);

    @GC
    public String primaryStorageInstallPath;
    @GC
    public VolumeVO volume;
    @GC
    public String primaryStorageUuid;

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (shouldCancal()) {
            completion.cancel();
            return;
        }

        PrimaryStorageVO ps = dbf.findByUuid(primaryStorageUuid, PrimaryStorageVO.class);
        if (ps == null) {
            completion.cancel();
            return;
        }
        if (ps.getStatus() != PrimaryStorageStatus.Connected) {
            completion.fail(operr("the primary storage[uuid:%s] is not in status of " +
                    "Connected, current status is %s", ps.getUuid(), ps.getStatus().toString()));
            return;
        }

        DeleteVolumeBitsOnPrimaryStorageMsg delMsg = new DeleteVolumeBitsOnPrimaryStorageMsg();
        delMsg.setInstallPath(primaryStorageInstallPath);
        delMsg.setBitsUuid(volume.getUuid());
        delMsg.setBitsType(VolumeVO.class.getSimpleName());
        delMsg.setPrimaryStorageUuid(primaryStorageUuid);
        delMsg.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(volume.getFormat()).toString());
        bus.makeTargetServiceIdByResourceUuid(delMsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(delMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private boolean shouldCancal() {
        if (EXECUTED_TIMES > PrimaryStorageGlobalConfig.PRIMARY_STORAGE_DELETEBITS_TIMES.value(Integer.class)) {
            logger.info(String.format("primary storage delete bits garbage canceled because execute more than %s times, " +
                    "you can delete it manually if needed, the primary storage installpath is: %s", EXECUTED_TIMES, primaryStorageInstallPath));
            return true;
        }
        return false;
    }
}