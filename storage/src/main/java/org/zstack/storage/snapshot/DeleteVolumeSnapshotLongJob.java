package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusImpl3;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.CancelDeleteVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.DeleteVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by kayo on 2018/5/9.
 */
@LongJobFor(APIDeleteVolumeSnapshotMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DeleteVolumeSnapshotLongJob implements LongJob {
    @Autowired
    protected CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        DeleteVolumeSnapshotMsg msg = new DeleteVolumeSnapshotMsg();
        APIDeleteVolumeSnapshotMsg apiMessage = JSONObjectUtil.toObject(job.getJobData(), APIDeleteVolumeSnapshotMsg.class);
        msg.setTreeUuid(apiMessage.getTreeUuid());
        msg.setVolumeUuid(apiMessage.getSnapshotUuid());
        msg.setSnapshotUuid(apiMessage.getSnapshotUuid());
        msg.setDeletionMode(apiMessage.getDeletionMode());
        msg.setDirection(apiMessage.getDirection());
        msg.setScope(apiMessage.getScope());
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeSnapshotConstant.SERVICE_ID, getRoutedMnId(apiMessage));
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(null);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private String getRoutedMnId(APIDeleteVolumeSnapshotMsg msg) {
        // ApiMsg has been set service id in their way, follow it.
        return CloudBusImpl3.getManagementNodeUUIDFromServiceID(msg.getServiceId());
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        APIDeleteVolumeSnapshotMsg apiMessage = JSONObjectUtil.toObject(job.getJobData(), APIDeleteVolumeSnapshotMsg.class);
        String volumeUuid = apiMessage.getVolumeUuid();

        ErrorCode error = checkIfVolumeSnapshotSupportCancel(volumeUuid);
        if (error != null) {
            completion.fail(error);
            return;
        }

        CancelDeleteVolumeSnapshotMsg cmsg = new CancelDeleteVolumeSnapshotMsg();
        cmsg.setSnapshotUuid(apiMessage.getSnapshotUuid());
        cmsg.setTreeUuid(apiMessage.getTreeUuid());
        cmsg.setVolumeUuid(volumeUuid);
        cmsg.setCancellationApiId(job.getApiId());
        String resourceUuid = cmsg.getVolumeUuid() != null ? cmsg.getVolumeUuid() : cmsg.getTreeUuid();
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(false);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private ErrorCode checkIfVolumeSnapshotSupportCancel(String volumeUuid) {
        VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
        if (volume == null) {
            return Platform.operr("failed to cancel deletion job. Volume[uuid:%s] not exists.");
        }

        if (volume.getVmInstanceUuid() == null) {
            return Platform.operr("failed to cancel deletion job. Volume[uuid:%s] not attached to any vm," +
                    " offline snapshot deletion do not support cancel.");
        }

        VmInstanceVO vmInstance = dbf.findByUuid(volume.getVmInstanceUuid(), VmInstanceVO.class);
        if (vmInstance == null) {
            return Platform.operr("failed to cancel deletion job. Volume[uuid:%s] attached vm not exists," +
                    " offline snapshot deletion do not support cancel.");
        }

        if (!vmInstance.getState().equals(VmInstanceState.Running)) {
            return Platform.operr("failed to cancel deletion job. Volume[uuid:%s] attached vm not in state %s" +
                    " offline snapshot deletion do not support cancel.", VmInstanceState.Running);
        }

        // if volume attached vm is Running state, allow cancel msg
        return null;
    }
}
