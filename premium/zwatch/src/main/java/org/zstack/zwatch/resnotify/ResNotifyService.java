package org.zstack.zwatch.resnotify;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.zstack.core.Platform;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.header.Component;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.migratedb.AuditsVO;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ResNotifyService implements Component {
    private static final CLogger logger = Utils.getLogger(ResNotifyService.class);

    // Reserved headers cannot be overridden by webhook customHeaders. Note: "authorization"
    // is intentionally NOT reserved -- subscribers (e.g. zcf vmauth pipeline) need to carry
    // their own Basic / Bearer credentials in customHeaders.
    private static final Set<String> RESERVED_HEADERS = new HashSet<>(Arrays.asList(
            "content-type", "x-zstack-signature", "x-zstack-event-type", "x-zstack-resource-type",
            "host"
    ));

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;

    public static final String EVENT_TYPE_CREATE = "CREATE";
    public static final String EVENT_TYPE_UPDATE = "UPDATE";
    public static final String EVENT_TYPE_DELETE = "DELETE";

    private EntityLifeCycleCallback createCallback;
    private EntityLifeCycleCallback updateCallback;
    private EntityLifeCycleCallback deleteCallback;
    private boolean callbacksInstalled = false;
    private ThreadPoolExecutor deliveryExecutor;
    private ScheduledExecutorService retryScheduler;

    private static final int DELIVERY_CORE_THREADS = 2;
    private static final int DELIVERY_MAX_THREADS = 4;
    private static final int DELIVERY_QUEUE_SIZE = 2048;
    private static final int WEBHOOK_MAX_RETRIES = 3;
    private static final long WEBHOOK_TIMEOUT_SECONDS = 30;

    @Override
    public boolean start() {
        deliveryExecutor = new ThreadPoolExecutor(
                DELIVERY_CORE_THREADS,
                DELIVERY_MAX_THREADS,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(DELIVERY_QUEUE_SIZE),
                namedThreadFactory("resnotify-delivery"),
                new ThreadPoolExecutor.AbortPolicy());
        deliveryExecutor.allowCoreThreadTimeOut(true);
        retryScheduler = Executors.newSingleThreadScheduledExecutor(namedThreadFactory("resnotify-retry"));

        createCallback = (evt, entity) -> handleEntityEvent(entity, EVENT_TYPE_CREATE);
        updateCallback = (evt, entity) -> handleEntityEvent(entity, EVENT_TYPE_UPDATE);
        deleteCallback = (evt, entity) -> handleEntityEvent(entity, EVENT_TYPE_DELETE);

        ZWatchGlobalConfig.RES_NOTIFY_ENABLE.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (newConfig.value(Boolean.class)) {
                    installCallbacks();
                } else {
                    uninstallCallbacks();
                }
            }
        });

        if (ZWatchGlobalConfig.RES_NOTIFY_ENABLE.value(Boolean.class)) {
            installCallbacks();
        }

        return true;
    }

    private ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + "-" + sequence.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }

    private synchronized void installCallbacks() {
        if (callbacksInstalled) {
            return;
        }

        logger.info("Installing resource notify entity lifecycle callbacks");
        dbf.installEntityLifeCycleCallback(null, EntityEvent.POST_PERSIST, createCallback);
        dbf.installEntityLifeCycleCallback(null, EntityEvent.POST_UPDATE, updateCallback);
        dbf.installEntityLifeCycleCallback(null, EntityEvent.POST_REMOVE, deleteCallback);
        callbacksInstalled = true;
    }

    private synchronized void uninstallCallbacks() {
        if (!callbacksInstalled) {
            return;
        }

        logger.info("Uninstalling resource notify entity lifecycle callbacks");
        dbf.uninstallEntityLifeCycleCallback(null, EntityEvent.POST_PERSIST, createCallback);
        dbf.uninstallEntityLifeCycleCallback(null, EntityEvent.POST_UPDATE, updateCallback);
        dbf.uninstallEntityLifeCycleCallback(null, EntityEvent.POST_REMOVE, deleteCallback);
        callbacksInstalled = false;
    }

    private boolean isNotifyCandidate(Object entity) {
        // Only notify for ResourceVO or AuditsVO, skip ResNotify's own entities
        return (entity instanceof ResourceVO || entity instanceof AuditsVO)
                && !(entity instanceof ResNotifySubscriptionVO)
                && !(entity instanceof ResNotifyWebhookRefVO);
    }

    private void handleEntityEvent(Object entity, String eventType) {
        if (!isNotifyCandidate(entity)) {
            return;
        }

        final String resourceType = normalizeResourceType(entity.getClass());
        final String snapshotPayload;
        final String resourceUuid;
        final Class<?> voClass;

        // Two delivery shapes, picked based on whether the row will still be
        // refetchable from a fresh session afterCommit:
        //
        // A) snapshot-in-hook (build payload synchronously now, deliver later):
        //      - DELETE events: row will be gone post-commit
        //      - AuditsVO: no String-uuid PK that dbf.findByUuid can use
        //        (its @Id is `long id`); also AuditsVO does NOT implement
        //        OwnedByAccount, so OwnedByAccountAspect never injects an
        //        in-flight IDENTITY AccountResourceRefVO at hook time and the
        //        autoflush hazard from ZSTAC-84863 does not apply. AuditsVO's
        //        Inventory.valueOf is a pure field copy (no SQL side effects),
        //        so building the payload synchronously inside the hook is safe.
        //
        // B) refetch-afterCommit (capture ref now, build payload later on a
        //    fresh session): for ResourceVO CREATE / UPDATE. This is the only
        //    shape that solves ZSTAC-84863, because Inventory.valueOf for
        //    OwnedByAccount + ResourceVO entities (e.g. IAM2ProjectInventory)
        //    can call query-triggering getters (vo.getLinkedAccountUuid ->
        //    SELECT IAM2ProjectAccountRefVO) that would autoflush and collide
        //    with the in-flight IDENTITY-id AccountResourceRefVO.
        if (mustSnapshotInHook(entity, eventType)) {
            snapshotPayload = buildEventPayload(entity, resourceType, eventType);
            if (snapshotPayload == null) {
                return;
            }
            resourceUuid = null;
            voClass = null;
        } else {
            resourceUuid = OwnedByAccount.getResourceUuid(entity);
            if (StringUtils.isEmpty(resourceUuid)) {
                return;
            }
            voClass = entity.getClass();
            snapshotPayload = null;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // afterCompletion is intentionally NOT overridden:
                    // STATUS_ROLLED_BACK / STATUS_UNKNOWN mean the entity
                    // changes are gone, so subscribers must not be notified.
                    enqueueDeferredDelivery(resourceType, resourceUuid, voClass, eventType, snapshotPayload);
                }
            });
        } else {
            // No active Spring synchronization (rare path -- e.g. callback
            // fired from a direct EntityManager flush outside @Transactional).
            // For shape A we already have the snapshot, so delivery proceeds
            // even cross-session. For shape B the dbf.findByUuid in
            // deliverDeferred may miss the row if the outer write hasn't
            // committed yet; that's the best we can do without a Spring
            // synchronization channel and the missing-row branch returns
            // safely without delivering a stale payload.
            enqueueDeferredDelivery(resourceType, resourceUuid, voClass, eventType, snapshotPayload);
        }
    }

    private String normalizeResourceType(Class<?> entityClass) {
        Class<?> current = entityClass;
        while (current != null && ResourceVO.class.isAssignableFrom(current)) {
            if (current.isAnnotationPresent(BaseResource.class)) {
                return current.getSimpleName();
            }
            current = current.getSuperclass();
        }

        return entityClass.getSimpleName();
    }

    // The row will be unavailable to a fresh session afterCommit -- either
    // because it's about to be deleted, or because the entity has no
    // String-uuid PK that dbf.findByUuid can resolve. In those cases we
    // must capture the payload synchronously while the entity is still in
    // hand. See handleEntityEvent for the full rationale + autoflush-safety
    // reasoning per entity shape.
    private boolean mustSnapshotInHook(Object entity, String eventType) {
        return EVENT_TYPE_DELETE.equals(eventType) || entity instanceof AuditsVO;
    }

    private void enqueueDeferredDelivery(String resourceType, String resourceUuid, Class<?> voClass,
                                         String eventType, String snapshotPayload) {
        try {
            deliveryExecutor.execute(() -> deliverDeferred(resourceType, resourceUuid, voClass, eventType, snapshotPayload));
        } catch (RejectedExecutionException e) {
            logger.warn(String.format("ResNotify delivery queue is full, dropping event[%s/%s]", eventType, resourceType), e);
        }
    }

    // Runs on the dedicated ResNotify delivery executor so subscription SELECT,
    // dbf.findByUuid and Inventory.valueOf execute outside the writer
    // transaction without consuming ThreadFacade's global worker pool.
    private void deliverDeferred(String resourceType, String resourceUuid, Class<?> voClass,
                                 String eventType, String snapshotPayload) {
        try {
            List<ResNotifySubscriptionVO> matchedSubs = selectMatchingSubscriptions(resourceType, eventType);
            if (matchedSubs.isEmpty()) {
                return;
            }

            String payload = snapshotPayload;
            if (payload == null) {
                Object freshVO = dbf.findByUuid(resourceUuid, voClass);
                if (freshVO == null) {
                    // Row vanished between commit and our delivery -- another
                    // concurrent delete; nothing useful to notify about.
                    return;
                }
                payload = buildEventPayload(freshVO, resourceType, eventType);
            }
            if (payload == null) {
                return;
            }
            for (ResNotifySubscriptionVO sub : matchedSubs) {
                deliverByType(sub, payload, eventType, resourceType);
            }
        } catch (Throwable t) {
            logger.warn("ResNotify deferred delivery failed: " + t.getMessage(), t);
        }
    }

    // Picks enabled subscriptions matching this (resourceType, eventType).
    // Querying directly each time keeps every MN consistent with the actual
    // subscription state (no per-node cache invalidation problem) and runs
    // on a worker thread + independent session, so the SELECT is safe.
    private List<ResNotifySubscriptionVO> selectMatchingSubscriptions(String resourceType, String eventType) {
        List<ResNotifySubscriptionVO> allSubs = Q.New(ResNotifySubscriptionVO.class)
                .eq(ResNotifySubscriptionVO_.state, ResNotifySubscriptionState.Enabled)
                .list();
        List<ResNotifySubscriptionVO> matched = new ArrayList<>();
        for (ResNotifySubscriptionVO sub : allSubs) {
            if (subscriptionMatchesRow(sub, resourceType, eventType)) {
                matched.add(sub);
            }
        }
        return matched;
    }

    private boolean subscriptionMatchesRow(ResNotifySubscriptionVO sub, String resourceType, String eventType) {
        if (StringUtils.isNotEmpty(sub.getResourceTypes())) {
            Set<String> types = splitCsvToSet(sub.getResourceTypes());
            if (!types.contains(resourceType)) {
                return false;
            }
        }
        if (StringUtils.isNotEmpty(sub.getEventTypes())) {
            Set<String> types = splitCsvToSet(sub.getEventTypes());
            if (!types.contains(eventType)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> splitCsvToSet(String value) {
        Set<String> ret = new HashSet<>();
        for (String item : value.split(",")) {
            if (StringUtils.isNotEmpty(item)) {
                ret.add(item.trim());
            }
        }
        return ret;
    }

    private String buildEventPayload(Object entity, String resourceType, String eventType) {
        try {
            Object inventory = ToInventory.toInventory(entity);
            if (inventory == null) {
                return null;
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventId", Platform.getUuid());
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("eventType", eventType);
            payload.put("resourceType", resourceType);
            payload.put("inventory", inventory);
            return JSONObjectUtil.toJsonString(payload);
        } catch (Exception e) {
            logger.debug(String.format("Failed to build payload for %s/%s: %s",
                    resourceType, eventType, e.getMessage()));
            return null;
        }
    }

    private void deliverByType(ResNotifySubscriptionVO sub, String payload, String eventType, String resourceType) {
        if (sub.getType() == ResNotifyType.WEBHOOK) {
            deliverWebhook(sub.getUuid(), payload, eventType, resourceType);
        }
    }

    private void deliverWebhook(String subscriptionUuid, String payload, String eventType, String resourceType) {
        ResNotifyWebhookRefVO webhookRef = dbf.findByUuid(subscriptionUuid, ResNotifyWebhookRefVO.class);
        if (webhookRef == null) {
            logger.warn(String.format("WebhookRef not found for subscription %s", subscriptionUuid));
            return;
        }

        String urlError = ResNotifyWebhookUrlValidator.validate(webhookRef.getWebhookUrl());
        if (urlError != null) {
            logger.warn(urlError);
            return;
        }

        Map<String, String> headers = new HashMap<>();

        // Apply custom headers first (filtered)
        if (StringUtils.isNotEmpty(webhookRef.getCustomHeaders())) {
            try {
                Map<String, String> customHeaders = JSONObjectUtil.toObject(webhookRef.getCustomHeaders(),
                        LinkedHashMap.class);
                if (customHeaders != null) {
                    for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                        if (!RESERVED_HEADERS.contains(entry.getKey().toLowerCase())) {
                            headers.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to parse custom headers: " + e.getMessage());
            }
        }

        // Security headers applied AFTER custom (cannot be overridden)
        headers.put("Content-Type", "application/json");
        headers.put("X-ZStack-Event-Type", eventType);
        headers.put("X-ZStack-Resource-Type", resourceType);

        if (StringUtils.isNotEmpty(webhookRef.getSecret())) {
            headers.put("X-ZStack-Signature", computeHmacSignature(payload, webhookRef.getSecret()));
        }

        String webhookUrl = webhookRef.getWebhookUrl();
        scheduleWebhookAttempt(webhookUrl, payload, headers, eventType, resourceType, 1, 0);
    }

    private void scheduleWebhookAttempt(String webhookUrl, String payload, Map<String, String> headers,
                                        String eventType, String resourceType, int attempt, long delayMs) {
        Runnable task = () -> deliverWebhookAttempt(webhookUrl, payload, headers, eventType, resourceType, attempt);
        try {
            if (delayMs <= 0) {
                deliveryExecutor.execute(task);
            } else {
                retryScheduler.schedule(() -> {
                    try {
                        deliveryExecutor.execute(task);
                    } catch (RejectedExecutionException e) {
                        logger.warn(String.format("ResNotify delivery queue is full, dropping retry event[%s/%s] to %s",
                                eventType, resourceType, webhookUrl), e);
                    }
                }, delayMs, TimeUnit.MILLISECONDS);
            }
        } catch (RejectedExecutionException e) {
            logger.warn(String.format("ResNotify delivery queue is full, dropping event[%s/%s] to %s",
                    eventType, resourceType, webhookUrl), e);
        }
    }

    private void deliverWebhookAttempt(String webhookUrl, String payload, Map<String, String> headers,
                                       String eventType, String resourceType, int attempt) {
        try {
            restf.syncJsonPost(webhookUrl, payload, headers, String.class, TimeUnit.SECONDS, WEBHOOK_TIMEOUT_SECONDS);
            logger.debug(String.format("Webhook delivered to %s for %s/%s (attempt %d)",
                    webhookUrl, eventType, resourceType, attempt));
        } catch (Exception e) {
            if (attempt >= WEBHOOK_MAX_RETRIES) {
                logger.warn(String.format("Webhook delivery failed to %s after %d attempts: %s",
                        webhookUrl, WEBHOOK_MAX_RETRIES, e.getMessage()));
                return;
            }

            long retryDelayMs = 1000L << (attempt - 1);
            logger.debug(String.format("Webhook delivery attempt %d failed for %s, retrying in %dms: %s",
                    attempt, webhookUrl, retryDelayMs, e.getMessage()));
            scheduleWebhookAttempt(webhookUrl, payload, headers, eventType, resourceType, attempt + 1, retryDelayMs);
        }
    }

    private String computeHmacSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return "sha256=" + hex;
        } catch (Exception e) {
            // HmacSHA256 is JCA-mandatory; should never happen
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }

    @Override
    public boolean stop() {
        uninstallCallbacks();
        if (retryScheduler != null) {
            retryScheduler.shutdownNow();
        }
        if (deliveryExecutor != null) {
            deliveryExecutor.shutdownNow();
        }
        return true;
    }
}
