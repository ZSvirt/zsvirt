package org.zstack.softwarePackage.compute.longjob;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.CancelDownloadFileOnBackupStorageHostMsg;
import org.zstack.softwarePackage.SoftwarePackageConstant;
import org.zstack.softwarePackage.compute.UploadSoftwarePackageToBackupStorageTracker;
import org.zstack.softwarePackage.entity.SoftwarePackageStatus;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO_;
import org.zstack.softwarePackage.entity.UploadSoftwarePackageToBackupStorageLongJobData;
import org.zstack.softwarePackage.header.*;
import org.zstack.softwarePackage.message.CleanSoftwarePackageMsg;
import org.zstack.softwarePackage.message.UploadSoftwarePackageToBackupStorageMsg;
import org.zstack.softwarePackage.message.UploadSoftwarePackageToBackupStorageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.err;
import static org.zstack.longjob.LongJobUtils.*;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.GENERAL_ERROR;

@LongJobFor(APIUploadSoftwarePackageToBackupStorageMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UploadSoftwarePackageToBackupStorageLongJob extends AbstractSoftwarePackageLongJob {
    private static final CLogger logger = Utils.getLogger(UploadSoftwarePackageToBackupStorageLongJob.class);

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIUploadSoftwarePackageToBackupStorageMsg apiMsg = JSONObjectUtil.toObject(job.getJobData(), APIUploadSoftwarePackageToBackupStorageMsg.class);

        UploadSoftwarePackageToBackupStorageMsg msg = UploadSoftwarePackageToBackupStorageMsg.fromApiMessage(apiMsg);
        job.setTargetResourceUuid(msg.getResourceUuid());
        job.setJobData(JSONObjectUtil.toJsonString(msg));
        databases.updateAndRefresh(job);

        auditResourceUuid = msg.getResourceUuid();

        APIUploadSoftwarePackageToBackupStorageEvent evt = new APIUploadSoftwarePackageToBackupStorageEvent(job.getApiId());
        SoftwarePackageLongJobCompletion comp = newCompletion(evt, evt::setInventory, job, completion);
        if (msg.needTrack()) {
            comp.startTrack("upload software package to backup storage");
        }

        bus.makeLocalServiceId(msg, SoftwarePackageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    handleSuccess(reply);
                } else {
                    auditResourceUuid = msg.getResourceUuid();
                    comp.fail(reply.getError());
                }
            }

            private void handleSuccess(MessageReply reply) {
                UploadSoftwarePackageToBackupStorageReply r = reply.castReply();
                auditResourceUuid = r.getInventory().getUuid();
                if (jobCanceled(job.getUuid())) {
                    cleanSoftwarePackage(msg.getResourceUuid(), comp, cancelErr(job.getUuid()));
                } else if (msg.needTrack()) {
                    comp.track(r.getInventory());
                } else {
                    comp.success(r.getInventory());
                }
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        UploadSoftwarePackageToBackupStorageMsg umsg = JSONObjectUtil.toObject(job.getJobData(), UploadSoftwarePackageToBackupStorageMsg.class);

        final String backupStorageUuid = getBackupStorageUuid(umsg.getResourceUuid());
        final String backupStorageHostUuid = getBackupStorageHostUuid(umsg.getResourceUuid());
        if (backupStorageUuid == null || backupStorageHostUuid == null) {
            completion.fail(err(GENERAL_ERROR,
                    "cancel UploadSoftwarePackageToBackupStorage: backupStorageUuid[%s] or backupStorageHostUuid[%s] is null",
                    backupStorageUuid, backupStorageHostUuid));
            return;
        }

        CancelDownloadFileOnBackupStorageHostMsg cmsg = new CancelDownloadFileOnBackupStorageHostMsg();
        cmsg.setBackupStorageUuid(backupStorageUuid);
        cmsg.setBackupStorageHostUuid(backupStorageHostUuid);
        cmsg.setCancellationApiId(job.getApiId());
        bus.makeTargetServiceIdByResourceUuid(cmsg, BackupStorageConstant.SERVICE_ID, backupStorageUuid);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(false);
                } else if (reply.getError().isError(SysErrors.RESOURCE_NOT_FOUND)) {
                    completion.success(true);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void resume(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        UploadSoftwarePackageToBackupStorageMsg msg = JSONObjectUtil.toObject(job.getJobData(), UploadSoftwarePackageToBackupStorageMsg.class);
        SoftwarePackageVO softwarePackageVO = Q.New(SoftwarePackageVO.class)
                .eq(SoftwarePackageVO_.uuid, msg.getResourceUuid())
                .find();

        if (softwarePackageVO == null) {
            completion.fail(err(SysErrors.RESOURCE_NOT_FOUND, "software package [uuid:%s] not found", msg.getResourceUuid()));
            return;
        }

        if (msg.needTrack()) {
            // Pre-checks BEFORE registering event listener to avoid double completion
            UploadSoftwarePackageToBackupStorageLongJobData msgData = UploadSoftwarePackageToBackupStorageLongJobData.buildFileLongJobDataFromMsg(msg);
            final String backupStorageUuid = getBackupStorageUuid(softwarePackageVO.getUuid());
            final String backupStorageHostUuid = getBackupStorageHostUuid(softwarePackageVO.getUuid());
            if (backupStorageUuid == null || backupStorageHostUuid == null) {
                completion.fail(err(GENERAL_ERROR, "resume UploadSoftwarePackageToBackupStorage backupStorageUuid[%s] or backupStorageHostUuid[%s] is null", backupStorageUuid, backupStorageHostUuid));
                return;
            }

            APIUploadSoftwarePackageToBackupStorageEvent evt = new APIUploadSoftwarePackageToBackupStorageEvent(job.getApiId());
            newCompletion(evt, evt::setInventory, job, completion).startTrack("upload software package to backup storage");

            msgData.backupStorageUuid = backupStorageUuid;
            msgData.backupStorageHostUuid = backupStorageHostUuid;
            new UploadSoftwarePackageToBackupStorageTracker().runTrackTask(msgData);
            return;
        }

        CleanSoftwarePackageMsg cmsg = new CleanSoftwarePackageMsg();
        cmsg.setUuid(msg.getResourceUuid());
        bus.makeLocalServiceId(cmsg, SoftwarePackageConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                softwarePackageVO.setStatus(SoftwarePackageStatus.UploadFailed.toString());
                databases.updateAndRefresh(softwarePackageVO);
                completion.fail(err(SysErrors.NOT_SUPPORTED, "resume non-track upload not supported"));
            }
        });
    }
}
