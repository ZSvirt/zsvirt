package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.VolumeBackupConstant;
import org.zstack.header.storage.backup.VolumeBackupInventory;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.backup.VolumeBackupVO_;
import org.zstack.header.storage.volume.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2019/3/8.
 */

@LongJobFor(APICreateVolumeBackupMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateVolumeBackupLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(CreateVolumeBackupLongJob.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        List<ErrorCode> errors = new ArrayList<>();
        APICreateVolumeBackupEvent evt = new APICreateVolumeBackupEvent(job.getApiId());
        VolumeBackupLongJobParams params = JSONObjectUtil.toObject(job.getJobData(), VolumeBackupLongJobParams.class);
        params.makeCompatibleParams();
        setAccountUuid(params, job.getAccountUuid());
        new While<>(params.getAlternativeBackupStorageUuids()).each((bsUuid, compl) -> {
            CreateVolumeBackupMsg msg = buildMsg(params);
            msg.setBackupStorageUuid(bsUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeBackupConstant.SERVICE_ID, bsUuid);
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        CreateVolumeBackupReply r = reply.castReply();
                        evt.setInventory(r.getInventory());
                        evt.setActualExecuteTime(r.getActualExecuteTime());
                        compl.allDone();
                    } else {
                        errors.add(reply.getError());
                        compl.done();
                    }
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (evt.getInventory() == null && !errors.isEmpty()) {
                    completion.fail(errors.get(0));
                    return;
                }
                if (params.getRemoteBackupStorageUuid() != null) {
                    syncBackupToRemoteBackupStorage(evt, params, completion);
                    return;
                }
                completion.success(evt);
            }
        });
    }

    private void setAccountUuid(VolumeBackupLongJobParams params, String accountUuid) {
        if (params.getAccountUuid() == null && accountUuid != null) {
            params.setAccountUuid(accountUuid);
        }
    }

    private void syncBackupToRemoteBackupStorage(APICreateVolumeBackupEvent evt, VolumeBackupLongJobParams params, ReturnValueCompletion<APIEvent> completion) {
        String backupStorageUuid = params.getBackupStorageUuid();
        String remoteBackupStorageUuid = params.getRemoteBackupStorageUuid();

        SyncBackupFromImageStoreBackupStorageMsg msg = new SyncBackupFromImageStoreBackupStorageMsg();
        msg.setUuid(evt.getInventory().getUuid());
        msg.setSrcBackupStorageUuid(backupStorageUuid);
        msg.setDstBackupStorageUuid(remoteBackupStorageUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeBackupConstant.SERVICE_ID, msg.getBackupStorageUuid());
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to sync backup from imageStoreBackupStorage[%s] to " +
                            "remoteImageStoreBackupStorage[%s], %s", backupStorageUuid, remoteBackupStorageUuid, reply.getError()));
                    return;
                }
                VolumeBackupVO vo = Q.New(VolumeBackupVO.class).eq(VolumeBackupVO_.uuid, evt.getInventory().getUuid()).find();
                evt.setInventory(VolumeBackupInventory.valueOf(vo));
                completion.success(evt);
            }
        });
    }

    private CreateVolumeBackupMsg buildMsg(VolumeBackupLongJobParams params) {
        CreateVolumeBackupMsg cmsg = new CreateVolumeBackupMsg();
        cmsg.setName(params.getName());
        cmsg.setVolumeUuid(params.getVolumeUuid());
        cmsg.setAccountUuid(params.getAccountUuid());
        cmsg.setBackupQosStruct(params.getBackupQosStruct());
        cmsg.setMode(params.getMode());
        return cmsg;
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        VolumeBackupLongJobParams params = JSONObjectUtil.toObject(job.getJobData(), VolumeBackupLongJobParams.class);

        CancelVolumeBackupJobMsg cmsg = new CancelVolumeBackupJobMsg();
        cmsg.setVolumeUuid(params.getVolumeUuid());
        bus.makeLocalServiceId(cmsg, VolumeBackupConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    completion.fail(r.getError());
                    return;
                }
                completion.success(true);
            }
        });
    }
}
