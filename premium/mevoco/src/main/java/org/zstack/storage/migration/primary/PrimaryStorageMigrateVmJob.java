package org.zstack.storage.migration.primary;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Constants;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.*;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.storage.migration.LongJobContextUtil;
import org.zstack.storage.migration.PrimaryStorageMigrateVmContext;
import org.zstack.storage.migration.PrimaryStorageMigrationRecoverHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by GuoYi on 12/7/17.
 */
@LongJobFor(APIPrimaryStorageMigrateVmMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageMigrateVmJob implements LongJob {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    protected String auditResourceUuid;
    private static final CLogger logger = Utils.getLogger(PrimaryStorageMigrateVmJob.class);

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        PrimaryStorageMigrateVmMsg msg = JSONObjectUtil.toObject(job.getJobData(), PrimaryStorageMigrateVmMsg.class);
        msg.setLongJobUuid(job.getUuid());
        msg.setJobContext(new PrimaryStorageMigrateVmContext());
        bus.makeLocalServiceId(msg, MevocoConstants.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess() || !reply.getError().isError(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR, LongJobErrors.INTERRUPTED)) {
                    SQL.New(LongJobVO.class).eq(LongJobVO_.uuid, job.getUuid()).set(LongJobVO_.jobResult, "").update();
                }
                if (reply.isSuccess()) {
                    PrimaryStorageMigrateVmReply r = reply.castReply();
                    APIPrimaryStorageMigrateVmEvent evt = new APIPrimaryStorageMigrateVmEvent(ThreadContext.get(Constants.THREAD_CONTEXT_API));

                    auditResourceUuid = r.getInventory().getUuid();
                    evt.setInventory(r.getInventory());
                    completion.success(evt);
                    return;
                }
                auditResourceUuid = msg.getVmInstanceUuid();
                completion.fail(reply.getError());
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        PrimaryStorageMigrateVmMsg msg = JSONObjectUtil.toObject(job.getJobData(), PrimaryStorageMigrateVmMsg.class);

        PrimaryStorageCancelMigrateVmMsg cmsg = new PrimaryStorageCancelMigrateVmMsg();
        cmsg.setDstPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
        cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        cmsg.setWithDataVolumes(msg.isWithDataVolumes());
        cmsg.setWithSnapshots(msg.isWithSnapshots());
        cmsg.setCancellationApiId(job.getApiId());
        cmsg.setDstHostUuid(msg.getDstHostUuid());
        cmsg.setSystemTags(msg.getSystemTags());
        bus.makeLocalServiceId(cmsg, MevocoConstants.SERVICE_ID);
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

    @Override
    public void clean(LongJobVO job, NoErrorCompletion completion) {
        PrimaryStorageMigrateVmMsg msg = JSONObjectUtil.toObject(job.getJobData(), PrimaryStorageMigrateVmMsg.class);

        List<String> incompleteVolUuids = Q.New(VolumeVO.class).select(VolumeVO_.uuid)
                .eq(VolumeVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(VolumeVO_.status, VolumeStatus.NotInstantiated)
                .gte(VolumeVO_.createDate, job.getCreateDate())
                .listValues();

        new While<>(incompleteVolUuids).step((volUuid, compl) -> {
            DeleteVolumeMsg dmsg = new DeleteVolumeMsg();
            dmsg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.name());
            dmsg.setUuid(volUuid);
            bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeConstant.SERVICE_ID, volUuid);
            bus.send(dmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        compl.addError(reply.getError());
                    }

                    compl.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    public void resume(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIPrimaryStorageMigrateVmMsg amsg = JSONObjectUtil.toObject(job.getJobData(), APIPrimaryStorageMigrateVmMsg.class);

        PrimaryStorageMigrateVmContext ctx = LongJobContextUtil.loadContext(job.getUuid(), PrimaryStorageMigrateVmContext.class);
        if (!PrimaryStorageMigrationRecoverHelper.needRecoverStorageMigration(ctx, amsg.getVmInstanceUuid())) {
            // no recovery required
            clean(job, new NoErrorCompletion() {
                @Override
                public void done() {
                    completion.fail(Platform.operr("vm[uuid:%s] storage migration long job[uuid:%s] failed because management node was restarted",
                            amsg.getVmInstanceUuid(), job.getUuid()));
                }
            });
            return;
        }

        logger.info(String.format("will resume job[%s] with context: %s", job.getUuid(), job.getJobResult()));
        PrimaryStorageMigrateVmMsg msg = new PrimaryStorageMigrateVmMsg(amsg);
        msg.setLongJobUuid(job.getUuid());
        msg.setJobContext(ctx);
        bus.makeTargetServiceIdByResourceUuid(msg, MevocoConstants.SERVICE_ID, msg.getVmInstanceUuid());
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                PrimaryStorageMigrateVmReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }
                APIPrimaryStorageMigrateVmEvent event = new APIPrimaryStorageMigrateVmEvent(job.getApiId());
                event.setInventory(reply1.getInventory());
                completion.success(event);
            }
        });
    }

    @Override
    public Class getAuditType() {
        return VmInstanceVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return auditResourceUuid;
    }
}
