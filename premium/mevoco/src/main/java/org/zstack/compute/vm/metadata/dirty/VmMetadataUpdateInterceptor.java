package org.zstack.compute.vm.metadata.dirty;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.longjob.*;
import org.zstack.longjob.LongJobExtensionPoint;

import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceStartExtensionPoint;
import org.zstack.header.vm.VmInstanceStartNewCreatedVmExtensionPoint;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.longjob.LongJobFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Intercepts {@link MetadataImpact @MetadataImpact}-annotated APIs and marks VM metadata
 * dirty when the API succeeds (or fails, if {@code updateOnFailure} is set).
 *
 * <h3>Dual-track tracking</h3>
 * <ul>
 *   <li><b>Direct API track</b> ({@link #inFlightApiTracking}):
 *       resolved on {@code APIEvent} via {@code beforePublishEvent}.</li>
 *   <li><b>LongJob track</b> ({@link #longJobApiTracking}):
 *       {@code APISubmitLongJobMsg}/{@code APIRerunLongJobMsg} wrap the real API;
 *       resolved on {@link LongJobExtensionPoint} callbacks when the job actually completes.</li>
 * </ul>
 *
 * <p>Annotation scanning, reflection caching, and VM UUID extraction are delegated to
 * {@link MetadataImpactApiRegistry}. Tag/config whitelist filtering is delegated to
 * {@link MetadataRelevanceFilter}.</p>
 */
public class VmMetadataUpdateInterceptor implements Component, ManagementNodeReadyExtensionPoint,
        VmInstanceStartExtensionPoint, VmInstanceStartNewCreatedVmExtensionPoint, LongJobExtensionPoint {

    private static final CLogger logger = Utils.getLogger(VmMetadataUpdateInterceptor.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private VmMetadataDirtyMarker dirtyMarker;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private LongJobFactory longJobFactory;
    @Autowired
    private DatabaseFacade dbf;

    // ---- helpers (POJO, not Spring beans) ----

    private MetadataImpactApiRegistry registry;
    private MetadataRelevanceFilter filter;

    // ---- dual-track API tracking ----

    /** Direct API track: keyed by API ID, resolved when APIEvent is published. */
    private final Map<String, TrackedApiContext> inFlightApiTracking = new ConcurrentHashMap<>();

    /** LongJob track: keyed by outer API ID (= LongJobVO.apiId), resolved on job completion. */
    private final Map<String, TrackedApiContext> longJobApiTracking = new ConcurrentHashMap<>();

    private final AtomicBoolean stalePendingCleanupStopped = new AtomicBoolean(false);

    // ---- Component lifecycle ----

    @Override
    public boolean start() {
        registry = new MetadataImpactApiRegistry();
        registry.scan();

        filter = new MetadataRelevanceFilter();

        // Intercept all @MetadataImpact APIs + LongJob wrapper APIs
        Set<Class<? extends APIMessage>> interceptedClasses = new HashSet<>(registry.getTrackedApiClasses());
        interceptedClasses.add(APISubmitLongJobMsg.class);
        interceptedClasses.add(APIRerunLongJobMsg.class);

        installBeforeDeliveryInterceptor(interceptedClasses);
        installBeforePublishEventInterceptor();

        return true;
    }

    @Override
    public boolean stop() {
        stopStalePendingCleanupTask();
        inFlightApiTracking.clear();
        longJobApiTracking.clear();
        return true;
    }

    @Override
    public void managementNodeReady() {
        startStalePendingCleanupTask();
    }

    // ---- interceptor installation ----

    private void installBeforeDeliveryInterceptor(Set<Class<? extends APIMessage>> interceptedClasses) {
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void beforeDeliveryMessage(Message msg) {
                if (!(msg instanceof APIMessage)) {
                    return;
                }
                if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
                    return;
                }

                APIMessage apiMsg = (APIMessage) msg;

                // ---- LongJob track ----
                // LongJob outer APIEvent fires at submission (job queued), NOT at completion,
                // so we must NOT put these in inFlightApiTracking.
                if (apiMsg instanceof APISubmitLongJobMsg || apiMsg instanceof APIRerunLongJobMsg) {
                    TrackedApiContext ctx = resolveLongJobInnerApi(apiMsg);
                    if (ctx != null) {
                        longJobApiTracking.put(apiMsg.getId(), ctx);
                    }
                    return;
                }

                // ---- Direct API track ----
                MetadataImpactApiRegistry.ApiResolverMeta meta = registry.getResolverMeta(msg.getClass());
                if (meta == null) {
                    return;
                }

                List<String> vmUuids = registry.extractVmUuidsFromApi(apiMsg, meta);
                if (vmUuids.isEmpty()) {
                    return;
                }

                // Content-level filtering: for Tag/Config APIs, skip if not whitelisted
                if (!filter.isRelevant(apiMsg)) {
                    logger.debug(String.format("[MetadataInterceptor] API %s (id=%s) tag/config not in whitelist, skipping",
                            msg.getClass().getSimpleName(), apiMsg.getId()));
                    return;
                }

                MetadataImpact impact = msg.getClass().getAnnotation(MetadataImpact.class);
                long apiTimeoutMs = apiMsg.getTimeout();
                if (apiTimeoutMs <= 0) {
                    logger.warn(String.format("[MetadataInterceptor] API %s (id=%s) has no resolved timeout, " +
                            "skip tracking. vmUuids=%s", msg.getClass().getSimpleName(), apiMsg.getId(), vmUuids));
                    return;
                }

                inFlightApiTracking.put(apiMsg.getId(),
                        new TrackedApiContext(vmUuids, impact.value(), impact.updateOnFailure(), apiTimeoutMs));
                logger.debug(String.format("[MetadataInterceptor] API %s (id=%s) tracked, " +
                                "field=%s, vmUuids=%s, impact=%s, updateOnFailure=%s, timeoutMs=%d",
                        msg.getClass().getSimpleName(), apiMsg.getId(), meta.fieldName,
                        vmUuids, impact.value(), impact.updateOnFailure(), apiTimeoutMs));
            }
        }, interceptedClasses.toArray(new Class[0]));
    }

    private void installBeforePublishEventInterceptor() {
        bus.installBeforePublishEventInterceptor(new AbstractBeforePublishEventInterceptor() {
            @Override
            public void beforePublishEvent(Event evt) {
                if (!(evt instanceof APIEvent)) {
                    return;
                }

                APIEvent apiEvent = (APIEvent) evt;
                TrackedApiContext ctx = inFlightApiTracking.remove(apiEvent.getApiId());

                if (ctx == null) {
                    // If the outer LongJob submission/rerun API itself failed (e.g. validation
                    // error, quota exceeded), the LongJob was never created, so afterJobFinished/
                    // afterJobFailed will never fire. Clean up to avoid lingering entries.
                    if (apiEvent.getError() != null) {
                        TrackedApiContext removed = longJobApiTracking.remove(apiEvent.getApiId());
                        if (removed != null) {
                            logger.debug(String.format("[MetadataInterceptor] LongJob outer API apiId=%s " +
                                            "failed before job creation, cleaned up longJobApiTracking. vmUuids=%s",
                                    apiEvent.getApiId(), removed.vmUuids));
                        }
                    }
                    return;
                }

                if (apiEvent.getError() != null && !ctx.updateOnFailure) {
                    logger.debug(String.format("[MetadataInterceptor] APIEvent apiId=%s " +
                                    "failed with error and updateOnFailure=false, skipping markDirty. vmUuids=%s",
                            apiEvent.getApiId(), ctx.vmUuids));
                    return;
                }

                for (String vmUuid : ctx.vmUuids) {
                    markDirtyForImpact(vmUuid, ctx.impact);
                }
            }
        });
    }

    // ---- LongJob resolution ----

    /**
     * Core resolution logic: given jobName + jobData, resolve the inner API class,
     * check @MetadataImpact, deserialize, and extract VM UUIDs.
     *
     * <p>Used by both the beforeDeliveryMessage fast path (pre-populate tracking)
     * and the handleLongJobCompletion fallback path (MN restart / takeover).</p>
     */
    private TrackedApiContext resolveFromJobNameAndData(String jobName, String jobData) {
        if (jobName == null || jobName.isEmpty()) {
            return null;
        }

        TreeMap<String, String> fullJobNames = longJobFactory.getFullJobName();
        String fullClassName = fullJobNames.get(jobName);
        if (fullClassName == null) {
            logger.debug(String.format("[MetadataInterceptor] LongJob jobName=%s has no mapping in LongJobFactory, skipping",
                    jobName));
            return null;
        }

        Class<?> innerApiClass;
        try {
            innerApiClass = Class.forName(fullClassName);
        } catch (ClassNotFoundException e) {
            logger.warn(String.format("[MetadataInterceptor] LongJob inner API class %s not found: %s",
                    fullClassName, e.getMessage()));
            return null;
        }

        MetadataImpact impact = innerApiClass.getAnnotation(MetadataImpact.class);
        if (impact == null || impact.value() == MetadataImpact.Impact.NONE) {
            return null;
        }

        MetadataImpactApiRegistry.ApiResolverMeta meta = registry.getResolverMeta(innerApiClass);
        if (meta == null) {
            logger.warn(String.format("[MetadataInterceptor] LongJob inner API %s has @MetadataImpact " +
                    "but no ApiResolverMeta registered (resolver/field misconfigured?)", fullClassName));
            return null;
        }

        if (jobData == null || jobData.isEmpty()) {
            logger.warn(String.format("[MetadataInterceptor] LongJob jobName=%s, jobData is empty, " +
                    "cannot extract vmUuids", jobName));
            return null;
        }

        APIMessage innerMsg;
        try {
            innerMsg = (APIMessage) JSONObjectUtil.toObject(jobData, innerApiClass);
        } catch (Exception e) {
            logger.warn(String.format("[MetadataInterceptor] failed to deserialize jobData for LongJob %s " +
                    "into %s: %s", jobName, fullClassName, e.getMessage()));
            return null;
        }

        // Apply the same tag/config whitelist filter as the direct API path
        if (!filter.isRelevant(innerMsg)) {
            logger.debug(String.format("[MetadataInterceptor] LongJob %s (innerApi=%s) tag/config not in whitelist, skipping",
                    jobName, innerApiClass.getSimpleName()));
            return null;
        }

        List<String> vmUuids = registry.extractVmUuidsFromApi(innerMsg, meta);
        if (vmUuids.isEmpty()) {
            return null;
        }

        return new TrackedApiContext(vmUuids, impact.value(), impact.updateOnFailure(), 0);
    }

    /**
     * Resolve inner API from LongJob submission/rerun messages.
     *
     * <p>Extracts jobName/jobData from the outer API message, then delegates to
     * {@link #resolveFromJobNameAndData(String, String)} for the actual resolution.</p>
     */
    private TrackedApiContext resolveLongJobInnerApi(APIMessage outerMsg) {
        String jobName;
        String jobData;

        if (outerMsg instanceof APISubmitLongJobMsg) {
            APISubmitLongJobMsg submitMsg = (APISubmitLongJobMsg) outerMsg;
            jobName = submitMsg.getJobName();
            jobData = submitMsg.getJobData();
        } else if (outerMsg instanceof APIRerunLongJobMsg) {
            APIRerunLongJobMsg rerunMsg = (APIRerunLongJobMsg) outerMsg;
            LongJobVO vo = dbf.findByUuid(rerunMsg.getLongJobUuid(), LongJobVO.class);
            if (vo == null) {
                logger.warn(String.format("[MetadataInterceptor] APIRerunLongJobMsg uuid=%s but LongJobVO not found",
                        rerunMsg.getLongJobUuid()));
                return null;
            }
            jobName = vo.getJobName();
            jobData = vo.getJobData();
        } else {
            return null;
        }

        TrackedApiContext ctx = resolveFromJobNameAndData(jobName, jobData);
        if (ctx != null) {
            logger.debug(String.format("[MetadataInterceptor] LongJob %s (innerApi) resolved vmUuids=%s, " +
                            "impact=%s, updateOnFailure=%s, outerApiId=%s",
                    jobName, ctx.vmUuids, ctx.impact, ctx.updateOnFailure, outerMsg.getId()));
        }
        return ctx;
    }

    // ---- LongJobExtensionPoint: mark dirty when the job actually completes ----

    private void handleLongJobCompletion(LongJobVO vo, boolean success) {
        if (vo == null || vo.getApiId() == null) {
            return;
        }

        if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
            longJobApiTracking.remove(vo.getApiId());
            return;
        }

        // Fast path: use cached tracking entry from beforeDeliveryMessage
        TrackedApiContext ctx = longJobApiTracking.remove(vo.getApiId());
        if (ctx == null) {
            // Fallback: tracking entry missing due to MN restart or job takeover by another MN.
            // Re-resolve from LongJobVO (persisted in DB) — stateless, works on any MN.
            ctx = resolveFromJobNameAndData(vo.getJobName(), vo.getJobData());
            if (ctx != null) {
                logger.info(String.format("[MetadataInterceptor] LongJob[uuid:%s] tracking miss, " +
                        "re-resolved from VO: vmUuids=%s", vo.getUuid(), ctx.vmUuids));
            }
        }

        if (ctx == null) {
            return;
        }

        if (!success && !ctx.updateOnFailure) {
            logger.debug(String.format("[MetadataInterceptor] LongJob[uuid:%s] failed and updateOnFailure=false, " +
                    "skipping markDirty. vmUuids=%s", vo.getUuid(), ctx.vmUuids));
            return;
        }

        for (String vmUuid : ctx.vmUuids) {
            markDirtyForImpact(vmUuid, ctx.impact);
        }
        logger.debug(String.format("[MetadataInterceptor] LongJob[uuid:%s, name:%s] completed (success=%s), " +
                "marked dirty for vmUuids=%s", vo.getUuid(), vo.getName(), success, ctx.vmUuids));
    }

    @Override
    public void afterJobFinished(LongJob job, LongJobVO vo, APIEvent evt) {
        handleLongJobCompletion(vo, true);
    }

    @Override
    public void afterJobFinished(LongJob job, LongJobVO vo) {
        handleLongJobCompletion(vo, true);
    }

    @Override
    public void afterJobFailed(LongJob job, LongJobVO vo, APIEvent evt) {
        handleLongJobCompletion(vo, false);
    }

    // ---- dirty marking ----

    void markDirtyForImpact(String vmInstanceUuid, MetadataImpact.Impact impact) {
        boolean storageStructureChange = (impact == MetadataImpact.Impact.STORAGE);
        logger.debug(String.format("[MetadataDirty] API succeeded, marking dirty for vm[uuid:%s], " +
                "impact=%s, storageStructureChange=%s", vmInstanceUuid, impact, storageStructureChange));
        dirtyMarker.markDirty(vmInstanceUuid, storageStructureChange);
    }

    // ---- TTL cleanup for inFlightApiTracking ----

    private synchronized void startStalePendingCleanupTask() {
        stalePendingCleanupStopped.set(false);

        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public long getInterval() {
                return 10;
            }

            @Override
            public String getName() {
                return "vm-metadata-pending-api-cleanup";
            }

            @Override
            public boolean run() {
                if (stalePendingCleanupStopped.get()) {
                    return true;
                }
                evictTimedOutEntries();
                return stalePendingCleanupStopped.get();
            }
        });
        logger.info("[MetadataInterceptor] pendingApis cleanup task started (check every 10min)");
    }

    private synchronized void stopStalePendingCleanupTask() {
        stalePendingCleanupStopped.set(true);
    }

    /**
     * Evict timed-out entries from {@link #inFlightApiTracking}, and
     * clean up stale {@link #longJobApiTracking} entries whose jobs have
     * already terminated (Canceled/Failed/Succeeded) without triggering
     * LongJobExtensionPoint callbacks (e.g. cancel path).
     */
    private void evictTimedOutEntries() {
        if (!inFlightApiTracking.isEmpty()) {
            long now = System.currentTimeMillis();
            int cleaned = 0;

            Iterator<Map.Entry<String, TrackedApiContext>> it = inFlightApiTracking.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, TrackedApiContext> entry = it.next();
                TrackedApiContext ctx = entry.getValue();
                long age = now - ctx.timestamp;

                if (age > ctx.apiTimeoutMs) {
                    it.remove();
                    // Timeout is ambiguous: the API may have succeeded but the Event was lost
                    // (bus fault, MN restart). Conservatively markDirty regardless of updateOnFailure
                    // to avoid silent metadata staleness.
                    for (String vmUuid : ctx.vmUuids) {
                        logger.warn(String.format("[MetadataInterceptor] inFlightApi timeout: apiId=%s, vm=%s, " +
                                        "age=%d min, apiTimeout=%d min, updateOnFailure=%s. Conservative markDirty applied.",
                                entry.getKey(), vmUuid, age / 60000, ctx.apiTimeoutMs / 60000, ctx.updateOnFailure));
                        markDirtyForImpact(vmUuid, ctx.impact);
                    }
                    cleaned++;
                }
            }

            if (cleaned > 0) {
                logger.info(String.format("[MetadataInterceptor] evicted %d timed-out inFlightApi entries", cleaned));
            }
        }

        // Clean up longJobApiTracking entries whose jobs have terminated
        // (Canceled, Failed, Succeeded) without calling LongJobExtensionPoint callbacks.
        if (!longJobApiTracking.isEmpty()) {
            int longJobCleaned = 0;
            Iterator<Map.Entry<String, TrackedApiContext>> ljIt = longJobApiTracking.entrySet().iterator();
            while (ljIt.hasNext()) {
                Map.Entry<String, TrackedApiContext> entry = ljIt.next();
                String apiId = entry.getKey();
                // Check if the LongJob with this apiId still exists and is in a terminal state
                LongJobState state = Q.New(LongJobVO.class).select(LongJobVO_.state)
                        .eq(LongJobVO_.apiId, apiId).findValue();
                if (state == null || state == LongJobState.Canceled
                        || state == LongJobState.Failed || state == LongJobState.Succeeded) {
                    ljIt.remove();
                    TrackedApiContext ctx = entry.getValue();
                    if (ctx != null && state != LongJobState.Succeeded) {
                        // state == null means the LongJob row is gone (MN restart, DB cleanup) —
                        // outcome is ambiguous, same as inFlightApi timeout. Conservatively markDirty.
                        // state == Failed/Canceled is a known failure — respect updateOnFailure flag.
                        boolean shouldMark = (state == null) || ctx.updateOnFailure;
                        if (shouldMark) {
                            for (String vmUuid : ctx.vmUuids) {
                                logger.warn(String.format("[MetadataInterceptor] longJob stale cleanup: apiId=%s, vm=%s, " +
                                        "jobState=%s, updateOnFailure=%s. Conservative markDirty applied.",
                                        apiId, vmUuid, state, ctx.updateOnFailure));
                                markDirtyForImpact(vmUuid, ctx.impact);
                            }
                        } else {
                            logger.debug(String.format("[MetadataInterceptor] longJob stale cleanup: apiId=%s, " +
                                    "jobState=%s, updateOnFailure=false, skip markDirty. vmUuids=%s",
                                    apiId, state, ctx.vmUuids));
                        }
                    }
                    longJobCleaned++;
                }
            }
            if (longJobCleaned > 0) {
                logger.info(String.format("[MetadataInterceptor] evicted %d stale longJobApiTracking entries", longJobCleaned));
            }
        }
    }

    // ---- VmInstanceStartExtensionPoint ----

    @Override
    public String preStartVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {
    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {
        if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
            return;
        }
        logger.debug(String.format("[MetadataInterceptor] VM started, marking dirty: vm[uuid:%s]", inv.getUuid()));
        try {
            dirtyMarker.markDirty(inv.getUuid());
        } catch (Exception e) {
            logger.warn(String.format("[MetadataInterceptor] failed to markDirty after VM started: vm[uuid:%s]", inv.getUuid()), e);
        }
    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {
    }

    // ---- VmInstanceStartNewCreatedVmExtensionPoint ----

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {
    }

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {
        if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
            return;
        }
        logger.debug(String.format("[MetadataInterceptor] new VM created, marking dirty: vm[uuid:%s]", inv.getUuid()));
        try {
            dirtyMarker.markDirty(inv.getUuid(), true);
        } catch (Exception e) {
            logger.warn(String.format("[MetadataInterceptor] failed to markDirty after new VM created: vm[uuid:%s]", inv.getUuid()), e);
        }
    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {
    }

    // ---- data class ----

    private static class TrackedApiContext {
        final List<String> vmUuids;
        final MetadataImpact.Impact impact;
        final boolean updateOnFailure;
        final long timestamp;
        final long apiTimeoutMs;

        TrackedApiContext(List<String> vmUuids, MetadataImpact.Impact impact, boolean updateOnFailure, long apiTimeoutMs) {
            this.vmUuids = vmUuids;
            this.impact = impact;
            this.updateOnFailure = updateOnFailure;
            this.timestamp = System.currentTimeMillis();
            this.apiTimeoutMs = apiTimeoutMs;
        }
    }
}
