package org.zstack.softwarePackage.compute.longjob;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.core.timeout.Timer;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.CancelHostTaskMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.longjob.LongJobGlobalConfig;
import org.zstack.longjob.LongJobUtils;
import org.zstack.softwarePackage.SoftwarePackageConstant;
import org.zstack.softwarePackage.compute.SoftwarePackageCanonicalEvents;
import org.zstack.softwarePackage.compute.UploadSoftwarePackageTracker;
import org.zstack.softwarePackage.entity.SoftwarePackageStatus;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO_;
import org.zstack.softwarePackage.header.*;
import org.zstack.softwarePackage.message.CleanSoftwarePackageMsg;
import org.zstack.softwarePackage.message.UploadSoftwarePackageMsg;
import org.zstack.softwarePackage.message.UploadSoftwarePackageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.core.Platform.err;
import static org.zstack.longjob.LongJobUtils.*;
import static org.zstack.longjob.LongJobUtils.cancelErr;
import static org.zstack.softwarePackage.SoftwarePackageConstant.SERVICE_ID;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED;
import static org.zstack.softwarePackage.compute.SoftwarePackageCanonicalEvents.SOFTWARE_PACKAGE_TRACK_RESULT_PATH;


@LongJobFor(APIUploadSoftwarePackageMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UploadSoftwarePackageLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(UploadSoftwarePackageLongJob.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    private Timer timer;
    @Autowired
    private TimeHelper timeHelper;

    protected String auditResourceUuid;

    class UploadSoftwarePackageCompletion<T extends SoftwarePackageInventory> extends ReturnValueCompletion<T> {
        APIUploadSoftwarePackageEvent event;
        LongJobVO job;
        ReturnValueCompletion<APIEvent> completion;
        AtomicBoolean done = new AtomicBoolean(false);

        UploadSoftwarePackageCompletion(APIUploadSoftwarePackageEvent event, LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
            super(completion);
            this.job = job;
            this.event = event;
            this.completion = completion;
        }

        @Override
        public void success(SoftwarePackageInventory softwarePackage) {
            if (done.compareAndSet(false, true)) {
                event.setInventory(softwarePackage);
                job = setJobResult(job.getUuid(), event);
                completion.success(event);
            }
        }

        @Override
        public void fail(ErrorCode err) {
            if (done.compareAndSet(false, true)) {
                job = setJobError(job.getUuid(), err);
                completion.fail(err);
            }
        }

        public synchronized void track(SoftwarePackageInventory softwarePackage) {
            if (!done.get()) {
                event.setInventory(softwarePackage);
                job = setJobResult(job.getUuid(), event);
            }
        }

        @SuppressWarnings("rawtypes")
        void startTrack() {
            long offTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.value(Long.class));
            evtf.on(SOFTWARE_PACKAGE_TRACK_RESULT_PATH, new AutoOffEventCallback<Object>() {
                @Override
                protected boolean run(Map tokens, Object d) {
                    SoftwarePackageCanonicalEvents.SoftwarePackageTrackData data = (SoftwarePackageCanonicalEvents.SoftwarePackageTrackData) d;
                    if (data.uuid.equals(job.getTargetResourceUuid())) {
                        handleResult(data);
                        return true;
                    }
                    return offTime < timeHelper.getCurrentTimeMillis();
                }

                private void handleResult(SoftwarePackageCanonicalEvents.SoftwarePackageTrackData data) {
                    if (data.success) {
                        success(data.inventory);
                    } else if (data.error.isError(UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED)) {
                        fail(LongJobUtils.interruptedErr(job.getUuid(), data.error));
                    } else {
                        fail(data.error);
                    }
                }
            });
        }
    }

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIUploadSoftwarePackageMsg apiMsg = JSONObjectUtil.toObject(job.getJobData(), APIUploadSoftwarePackageMsg.class);

        UploadSoftwarePackageMsg msg = UploadSoftwarePackageMsg.fromApiMessage(apiMsg);
        job.setTargetResourceUuid(msg.getResourceUuid());
        job.setJobData(JSONObjectUtil.toJsonString(msg));
        dbf.updateAndRefresh(job);

        auditResourceUuid = msg.getResourceUuid();

        APIUploadSoftwarePackageEvent evt = new APIUploadSoftwarePackageEvent(job.getApiId());
        UploadSoftwarePackageCompletion comp = new UploadSoftwarePackageCompletion(evt, job, completion);
        if (msg.needTrack()) {
            comp.startTrack();
        }

        bus.makeServiceIdByManagementNodeId(msg, SoftwarePackageConstant.SERVICE_ID, msg.getManagementNodeUuid());
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
                UploadSoftwarePackageReply r = reply.castReply();
                auditResourceUuid = r.getInventory().getUuid();
                if (jobCanceled(job.getUuid())) {
                    cleanSoftwarePackage(msg, comp, cancelErr(job.getUuid()));
                } else if (msg.needTrack()) {
                    comp.track(r.getInventory());
                } else {
                    comp.success(r.getInventory());
                }
            }
        });
    }

    private void cleanSoftwarePackage(UploadSoftwarePackageMsg msg, UploadSoftwarePackageCompletion completion, ErrorCode err) {
        CleanSoftwarePackageMsg cmsg = new CleanSoftwarePackageMsg();
        cmsg.setUuid(msg.getResourceUuid());
        bus.makeServiceIdByManagementNodeId(cmsg, SERVICE_ID, msg.getManagementNodeUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                completion.fail(err);
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        UploadSoftwarePackageMsg umsg = JSONObjectUtil.toObject(job.getJobData(), UploadSoftwarePackageMsg.class);

        SQL.New(SoftwarePackageVO.class)
                .eq(SoftwarePackageVO_.uuid, umsg.getResourceUuid())
                .set(SoftwarePackageVO_.status, SoftwarePackageStatus.UploadFailed.toString())
                .update();

        CancelHostTaskMsg cmsg = new CancelHostTaskMsg();
        cmsg.setHostUuid(umsg.getHostUuid());
        cmsg.setCancellationApiId(job.getApiId());
        bus.makeLocalServiceId(cmsg, HostConstant.SERVICE_ID);
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
        UploadSoftwarePackageMsg msg = JSONObjectUtil.toObject(job.getJobData(), UploadSoftwarePackageMsg.class);
        SoftwarePackageVO softwarePackageVO = Q.New(SoftwarePackageVO.class).eq(SoftwarePackageVO_.uuid, msg.getResourceUuid()).find();

        if (softwarePackageVO == null) {
            completion.fail(err(SysErrors.RESOURCE_NOT_FOUND, "software package [uuid:%s] not found", msg.getResourceUuid()));
            return;
        }

        if (!Objects.equals(Platform.getManagementServerId(), softwarePackageVO.getManagementNodeUuid())) {
            softwarePackageVO.setStatus(SoftwarePackageStatus.UploadFailed.toString());
            dbf.updateAndRefresh(softwarePackageVO);
            completion.fail(err(SysErrors.NOT_SUPPORTED, "not support"));
            return;
        }

        if (msg.needTrack()) {
            APIUploadSoftwarePackageEvent evt = new APIUploadSoftwarePackageEvent(job.getApiId());
            new UploadSoftwarePackageCompletion(evt, job, completion).startTrack();
            new UploadSoftwarePackageTracker().runTrackTask(softwarePackageVO.getUuid(), msg.getHostUuid());
            return;
        }

        softwarePackageVO.setStatus(SoftwarePackageStatus.UploadFailed.toString());
        dbf.updateAndRefresh(softwarePackageVO);
        completion.fail(err(SysErrors.NOT_SUPPORTED, "not support"));
    }

    @Override
    public void clean(LongJobVO job, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public Class<?> getAuditType() {
        return SoftwarePackageVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return auditResourceUuid;
    }
}
