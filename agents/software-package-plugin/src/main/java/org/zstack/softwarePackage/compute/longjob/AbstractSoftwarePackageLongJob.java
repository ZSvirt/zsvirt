package org.zstack.softwarePackage.compute.longjob;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.longjob.LongJobGlobalConfig;
import org.zstack.longjob.LongJobUtils;
import org.zstack.softwarePackage.SoftwarePackageConstant;
import org.zstack.softwarePackage.compute.SoftwarePackageCanonicalEvents;
import org.zstack.softwarePackage.compute.SoftwarePackageSystemTags;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.header.SoftwarePackageInventory;
import org.zstack.softwarePackage.message.CleanSoftwarePackageMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.zstack.core.Platform.err;
import static org.zstack.longjob.LongJobUtils.setJobError;
import static org.zstack.longjob.LongJobUtils.setJobResult;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED;
import static org.zstack.softwarePackage.compute.SoftwarePackageCanonicalEvents.SOFTWARE_PACKAGE_TRACK_RESULT_PATH;

public abstract class AbstractSoftwarePackageLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(AbstractSoftwarePackageLongJob.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade databases;
    @Autowired
    protected EventFacade events;
    @Autowired
    protected ThreadFacade threads;

    protected String auditResourceUuid;
    protected volatile SoftwarePackageLongJobCompletion activeCompletion;

    protected class SoftwarePackageLongJobCompletion extends ReturnValueCompletion<SoftwarePackageInventory> {
        protected final APIEvent event;
        protected LongJobVO job;
        protected final ReturnValueCompletion<APIEvent> completion;
        protected final AtomicBoolean done = new AtomicBoolean(false);
        private final Object lock = new Object();

        private final Consumer<SoftwarePackageInventory> inventorySetter;
        private volatile AutoOffEventCallback<Object> registeredCallback;

        protected SoftwarePackageLongJobCompletion(APIEvent event, Consumer<SoftwarePackageInventory> inventorySetter,
                                                   LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
            super(completion);
            this.event = event;
            this.inventorySetter = inventorySetter;
            this.job = job;
            this.completion = completion;
        }

        @Override
        public void success(SoftwarePackageInventory softwarePackage) {
            synchronized (lock) {
                if (done.compareAndSet(false, true)) {
                    offRegisteredCallback();
                    inventorySetter.accept(softwarePackage);
                    job = setJobResult(job.getUuid(), event);
                    completion.success(event);
                }
            }
        }

        @Override
        public void fail(ErrorCode err) {
            synchronized (lock) {
                if (done.compareAndSet(false, true)) {
                    offRegisteredCallback();
                    job = setJobError(job.getUuid(), err);
                    completion.fail(err);
                }
            }
        }

        private void offRegisteredCallback() {
            AutoOffEventCallback<Object> cb = registeredCallback;
            if (cb != null) {
                events.off(cb);
            }
        }

        /**
         * Save intermediate progress. Synchronized with success()/fail() to prevent
         * overwriting a terminal state (e.g., failure) with an intermediate result.
         */
        public void track(SoftwarePackageInventory softwarePackage) {
            synchronized (lock) {
                if (!done.get()) {
                    inventorySetter.accept(softwarePackage);
                    job = setJobResult(job.getUuid(), event);
                }
            }
        }

        /**
         * Register an event listener that tracks the software package operation result.
         * The listener also implements a timeout mechanism: if no matching event arrives
         * before the deadline, the job is failed with a timeout error on the next
         * unrelated event delivery.
         * <p>
         * IMPORTANT: handleResult() is checked BEFORE the timeout condition so that a
         * successful event arriving at the exact timeout boundary is not lost.
         */
        @SuppressWarnings("rawtypes")
        public void startTrack(String timeoutDescription) {
            long timeoutMs = TimeUnit.SECONDS.toMillis(LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.value(Long.class));
            long offTime = System.currentTimeMillis() + timeoutMs;
            final AutoOffEventCallback<Object> callback = new AutoOffEventCallback<Object>() {
                @Override
                protected boolean run(Map tokens, Object d) {
                    SoftwarePackageCanonicalEvents.SoftwarePackageTrackData data = (SoftwarePackageCanonicalEvents.SoftwarePackageTrackData) d;
                    // Check matching result first — a successful event should never be
                    // discarded just because the clock happened to cross the timeout boundary.
                    if (data.uuid.equals(job.getTargetResourceUuid())) {
                        handleResult(data);
                        return true;
                    }
                    if (System.currentTimeMillis() > offTime) {
                        fail(err(SysErrors.TIMEOUT, "%s timed out for job[uuid:%s]", timeoutDescription, job.getUuid()));
                        return true;
                    }
                    return false;
                }

                private void handleResult(SoftwarePackageCanonicalEvents.SoftwarePackageTrackData data) {
                    if (data.success) {
                        SoftwarePackageLongJobCompletion.this.success(data.inventory);
                    } else if (data.error.isError(UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED)) {
                        SoftwarePackageLongJobCompletion.this.fail(LongJobUtils.interruptedErr(job.getUuid(), data.error));
                    } else {
                        SoftwarePackageLongJobCompletion.this.fail(data.error);
                    }
                }
            };
            registeredCallback = callback;
            events.on(SOFTWARE_PACKAGE_TRACK_RESULT_PATH, callback);

            // Independent timeout: fires even if no events arrive on the path,
            // preventing the LongJob from hanging indefinitely.
            // NOTE: do NOT hold lock here — fail() has its own synchronization.
            // Holding lock would risk deadlock if evtf.off() or completion.fail()
            // contend with other lock holders.
            threads.submitTimeoutTask(() -> {
                if (!done.get()) {
                    events.off(callback);
                    fail(err(SysErrors.TIMEOUT, "%s timed out for job[uuid:%s] (no events received)", timeoutDescription, job.getUuid()));
                }
            }, TimeUnit.MILLISECONDS, timeoutMs);
        }
    }

    protected SoftwarePackageLongJobCompletion newCompletion(APIEvent event, Consumer<SoftwarePackageInventory> inventorySetter,
                                                            LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        SoftwarePackageLongJobCompletion comp = new SoftwarePackageLongJobCompletion(event, inventorySetter, job, completion);
        activeCompletion = comp;
        return comp;
    }

    protected void cleanSoftwarePackage(String softwarePackageUuid, SoftwarePackageLongJobCompletion completion, ErrorCode err) {
        CleanSoftwarePackageMsg cmsg = new CleanSoftwarePackageMsg();
        cmsg.setUuid(softwarePackageUuid);
        bus.makeLocalServiceId(cmsg, SoftwarePackageConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                completion.fail(err);
            }
        });
    }

    protected String getBackupStorageUuid(String softwarePackageUuid) {
        return SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID
                .getTokenByResourceUuid(softwarePackageUuid, SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN);
    }

    protected String getBackupStorageHostUuid(String softwarePackageUuid) {
        return SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID
                .getTokenByResourceUuid(softwarePackageUuid, SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN);
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
