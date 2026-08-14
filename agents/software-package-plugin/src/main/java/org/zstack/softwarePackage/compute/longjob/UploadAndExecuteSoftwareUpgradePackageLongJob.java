package org.zstack.softwarePackage.compute.longjob;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
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
import org.zstack.softwarePackage.message.UploadAndExecuteSoftwareUpgradePackageMsg;
import org.zstack.softwarePackage.message.UploadAndExecuteSoftwareUpgradePackageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.err;
import static org.zstack.longjob.LongJobUtils.cancelErr;
import static org.zstack.longjob.LongJobUtils.jobCanceled;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.GENERAL_ERROR;

@LongJobFor(APIUploadAndExecuteSoftwareUpgradePackageMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UploadAndExecuteSoftwareUpgradePackageLongJob extends AbstractSoftwarePackageLongJob {
    private static final CLogger logger = Utils.getLogger(UploadAndExecuteSoftwareUpgradePackageLongJob.class);

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIUploadAndExecuteSoftwareUpgradePackageMsg apiMsg = JSONObjectUtil.toObject(job.getJobData(), APIUploadAndExecuteSoftwareUpgradePackageMsg.class);

        UploadAndExecuteSoftwareUpgradePackageMsg msg = UploadAndExecuteSoftwareUpgradePackageMsg.fromApiMessage(apiMsg);

        job.setTargetResourceUuid(msg.getSoftwarePackageUuid());
        job.setJobData(JSONObjectUtil.toJsonString(msg));
        databases.updateAndRefresh(job);

        auditResourceUuid = msg.getSoftwarePackageUuid();

        APIUploadAndExecuteSoftwareUpgradePackageEvent evt = new APIUploadAndExecuteSoftwareUpgradePackageEvent(job.getApiId());
        SoftwarePackageLongJobCompletion comp = newCompletion(evt, evt::setInventory, job, completion);
        if (msg.needTrack()) {
            comp.startTrack("upload and execute software upgrade package");
        }

        bus.makeLocalServiceId(msg, SoftwarePackageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    handleSuccess(reply);
                } else {
                    auditResourceUuid = msg.getSoftwarePackageUuid();
                    comp.fail(reply.getError());
                }
            }

            private void handleSuccess(MessageReply reply) {
                UploadAndExecuteSoftwareUpgradePackageReply r = reply.castReply();
                auditResourceUuid = r.getInventory().getUuid();
                if (jobCanceled(job.getUuid())) {
                    cleanSoftwarePackage(msg.getSoftwarePackageUuid(), comp, cancelErr(job.getUuid()));
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
        UploadAndExecuteSoftwareUpgradePackageMsg umsg = JSONObjectUtil.toObject(job.getJobData(), UploadAndExecuteSoftwareUpgradePackageMsg.class);

        final String backupStorageUuid = getBackupStorageUuid(umsg.getSoftwarePackageUuid());
        final String backupStorageHostUuid = getBackupStorageHostUuid(umsg.getSoftwarePackageUuid());
        if (backupStorageUuid == null || backupStorageHostUuid == null) {
            completion.fail(err(GENERAL_ERROR,
                    "cancel UploadAndExecuteSoftwareUpgradePackage: backupStorageUuid[%s] or backupStorageHostUuid[%s] is null",
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
        UploadAndExecuteSoftwareUpgradePackageMsg msg = JSONObjectUtil.toObject(job.getJobData(), UploadAndExecuteSoftwareUpgradePackageMsg.class);
        SoftwarePackageVO softwarePackageVO = Q.New(SoftwarePackageVO.class)
                .eq(SoftwarePackageVO_.uuid, msg.getSoftwarePackageUuid())
                .find();

        if (softwarePackageVO == null) {
            completion.fail(err(SysErrors.RESOURCE_NOT_FOUND, "software package [uuid:%s] not found", msg.getSoftwarePackageUuid()));
            return;
        }

        if (msg.needTrack()) {
            UploadSoftwarePackageToBackupStorageLongJobData msgData = UploadSoftwarePackageToBackupStorageLongJobData.buildFileLongJobDataFromMsg(msg);
            final String backupStorageUuid = getBackupStorageUuid(softwarePackageVO.getUuid());
            final String backupStorageHostUuid = getBackupStorageHostUuid(softwarePackageVO.getUuid());
            if (backupStorageUuid == null || backupStorageHostUuid == null) {
                completion.fail(err(GENERAL_ERROR,
                        "resume UploadSoftwarePackageToBackupStorage backupStorageUuid[%s] or backupStorageHostUuid[%s] is null", backupStorageUuid, backupStorageHostUuid));
                return;
            }

            APIUploadAndExecuteSoftwareUpgradePackageEvent evt = new APIUploadAndExecuteSoftwareUpgradePackageEvent(job.getApiId());
            newCompletion(evt, evt::setInventory, job, completion).startTrack("upload and execute software upgrade package");

            msgData.backupStorageUuid = backupStorageUuid;
            msgData.backupStorageHostUuid = backupStorageHostUuid;

            UploadSoftwarePackageToBackupStorageTracker tracker = new UploadSoftwarePackageToBackupStorageTracker();
            tracker.setUpgrade(true);
            tracker.runTrackTask(msgData);
            return;
        }

        // If upload already completed (UpgradePackageUploaded or UpgradeExecuteFailed),
        // the upgrade package on backup storage is needed for Reexecute — do NOT clean it.
        // Only clean when still in upload phase (Upgrading).
        String currentStatus = softwarePackageVO.getStatus();
        if (SoftwarePackageStatus.UpgradePackageUploaded.toString().equals(currentStatus)
                || SoftwarePackageStatus.UpgradeExecuteFailed.toString().equals(currentStatus)) {
            SQL.New(SoftwarePackageVO.class)
                    .eq(SoftwarePackageVO_.uuid, msg.getSoftwarePackageUuid())
                    .set(SoftwarePackageVO_.status, SoftwarePackageStatus.UpgradeExecuteFailed.toString())
                    .update();
            completion.fail(err(SysErrors.NOT_SUPPORTED, "resume non-track upload not supported"));
            return;
        }

        completion.fail(err(SysErrors.NOT_SUPPORTED, "resume non-track upload not supported, please clean upgrade package and retry"));
    }
}
