package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.volume.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/3/8.
 */

@LongJobFor(APICreateVmBackupMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateVmBackupLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(CreateVmBackupLongJob.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        List<ErrorCode> errors = new ArrayList<>();
        APICreateVmBackupEvent evt = new APICreateVmBackupEvent(job.getApiId());
        VmBackupLongJobParams params = JSONObjectUtil.toObject(job.getJobData(), VmBackupLongJobParams.class);
        params.makeCompatibleParams();
        setAccountUuid(params, job.getAccountUuid());
        new While<>(params.getAlternativeBackupStorageUuids()).each((bsUuid, compl) -> {
            CreateVmBackupMsg msg = buildMsg(params);
            msg.setBackupStorageUuid(bsUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeBackupConstant.SERVICE_ID, bsUuid);
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        CreateVmBackupReply r = reply.castReply();
                        evt.setInventories(r.getInventories());
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
                if (evt.getInventories() == null && !errors.isEmpty()) {
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

    private void setAccountUuid(VmBackupLongJobParams params, String accountUuid) {
        if (params.getAccountUuid() == null && accountUuid != null) {
            params.setAccountUuid(accountUuid);
        }
    }

    private void syncBackupToRemoteBackupStorage(APICreateVmBackupEvent evt, VmBackupLongJobParams params, ReturnValueCompletion<APIEvent> completion) {
        String backupStorageUuid = params.getBackupStorageUuid();
        String remoteBackupStorageUuid = params.getRemoteBackupStorageUuid();

        List<SyncBackupFromImageStoreBackupStorageMsg> msgs = new ArrayList<>();
        for (VolumeBackupInventory inv : evt.getInventories()) {
            SyncBackupFromImageStoreBackupStorageMsg msg = new SyncBackupFromImageStoreBackupStorageMsg();
            msg.setUuid(inv.getUuid());
            msg.setSrcBackupStorageUuid(backupStorageUuid);
            msg.setDstBackupStorageUuid(remoteBackupStorageUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeBackupConstant.SERVICE_ID, msg.getBackupStorageUuid());
            msgs.add(msg);
        }

        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to sync backup from imageStoreBackupStorage[%s] to " +
                            "remoteImageStoreBackupStorage[%s], %s", backupStorageUuid, remoteBackupStorageUuid, reply.getError()));
                }
                com.done();
            }
        })).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                List<String> voUuids = evt.getInventories().stream().map(VolumeBackupInventory::getUuid).collect(Collectors.toList());
                List<VolumeBackupVO> vos = Q.New(VolumeBackupVO.class).in(VolumeBackupVO_.uuid, voUuids).list();
                evt.setInventories(VolumeBackupInventory.valueOf(vos));
                completion.success(evt);
            }
        });
    }

    private CreateVmBackupMsg buildMsg(VmBackupLongJobParams params) {
        CreateVmBackupMsg cmsg = new CreateVmBackupMsg();
        cmsg.setName(params.getName());
        cmsg.setRootVolumeUuid(params.getRootVolumeUuid());
        cmsg.setAccountUuid(params.getAccountUuid());
        cmsg.setBackupQosStruct(params.getBackupQosStruct());
        cmsg.setMode(params.getMode());
        cmsg.setResourceUuid(Platform.getUuid());
        return cmsg;
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        VmBackupLongJobParams params = JSONObjectUtil.toObject(job.getJobData(), VmBackupLongJobParams.class);

        CancelVmBackupJobMsg cmsg = new CancelVmBackupJobMsg();
        cmsg.setVolumeUuid(params.getRootVolumeUuid());
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
