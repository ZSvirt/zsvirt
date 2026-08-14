package org.zstack.crypto.keyprovider.kms;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.crypto.keyprovider.KmipConfig;
import org.zstack.crypto.keyprovider.KeyToolGrpcClient;
import org.zstack.header.Component;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.keyprovider.KeyProviderErrors;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderUtils;
import org.zstack.header.keyprovider.KmsIdentityVO;
import org.zstack.header.keyprovider.KmsIdentityVO_;
import org.zstack.header.keyprovider.KmsTrustState;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.keyprovider.KmsVO_;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class KmsHealthTracker implements Component, ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener {
    private static final CLogger logger = Utils.getLogger(KmsHealthTracker.class);
    private static final long DEFAULT_HEALTH_INTERVAL_SECONDS = 60L;
    private static final int MAX_UNKNOWN_FAILURES = 3;

    private final Map<String, Tracker> trackers = new ConcurrentHashMap<>();
    private final Map<String, Integer> unknownFailureCounts = new ConcurrentHashMap<>();
    private final Object trackerLock = new Object();

    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void managementNodeReady() {
        reScanKms();
    }

    @Override
    @AsyncThread
    public void nodeJoin(ManagementNodeInventory inv) {
        reScanKms();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        reScanKms();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        synchronized (trackerLock) {
            trackers.values().forEach(Tracker::cancel);
            trackers.clear();
            unknownFailureCounts.clear();
        }
        return true;
    }

    public void trackKms(String kmsUuid) {
        if (StringUtils.isBlank(kmsUuid) || !destMaker.isManagedByUs(kmsUuid)) {
            return;
        }

        Tracker tracker;
        Tracker existing;
        synchronized (trackerLock) {
            existing = trackers.get(kmsUuid);
            if (existing != null) {
                existing.cancel();
            }

            tracker = new Tracker(kmsUuid);
            trackers.put(kmsUuid, tracker);
        }
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            tracker.start();
        } else {
            tracker.startRightNow();
        }
    }

    public void untrackKms(String kmsUuid) {
        Tracker tracker;
        synchronized (trackerLock) {
            tracker = trackers.remove(kmsUuid);
        }
        unknownFailureCounts.remove(kmsUuid);
        if (tracker != null) {
            tracker.cancel();
        }
    }

    public void triggerHealthCheck(String kmsUuid) {
        trackKms(kmsUuid);
    }

    private void reScanKms() {
        synchronized (trackerLock) {
            new ArrayList<>(trackers.values()).forEach(Tracker::cancel);
            trackers.clear();
            unknownFailureCounts.clear();
        }

        Set<String> toTrack = Q.New(KmsVO.class)
                .select(KmsVO_.uuid)
                .listValues()
                .stream()
                .map(String::valueOf)
                .filter(uuid -> destMaker.isManagedByUs(uuid))
                .collect(Collectors.toSet());
        toTrack.forEach(this::trackKms);
    }

    private class Tracker extends AsyncTimer {
        private final String kmsUuid;

        Tracker(String kmsUuid) {
            super(TimeUnit.SECONDS, DEFAULT_HEALTH_INTERVAL_SECONDS);
            this.kmsUuid = kmsUuid;
            __name__ = String.format("kms-health-tracker-%s", kmsUuid);
        }

        @Override
        protected void execute() {
            checkHealth();
        }

        private void checkHealth() {
            thdf.chainSubmit(new ChainTask(new AsyncBackup() {
            }) {
                @Override
                public String getSyncSignature() {
                    return KeyProviderUtils.getSyncSignature(KeyProviderType.KMS, kmsUuid);
                }

                @Override
                public void run(SyncTaskChain chain) {
                    try {
                        doHealthCheck(kmsUuid, Tracker.this);
                    } finally {
                        chain.next();
                        Tracker.this.continueToRunThisTimer();
                    }
                }

                @Override
                public String getName() {
                    return String.format("kms-health-check-%s", kmsUuid);
                }
            });
        }
    }

    private void doHealthCheck(String kmsUuid, Tracker runningTracker) {
        if (shouldDiscardHealthCheckResult(kmsUuid, runningTracker)) {
            return;
        }

        KmsVO kms = dbf.findByUuid(kmsUuid, KmsVO.class);
        if (kms == null) {
            untrackKms(kmsUuid);
            return;
        }

        List<KmsIdentityVO> identities = Q.New(KmsIdentityVO.class)
                .eq(KmsIdentityVO_.kmsUuid, kmsUuid)
                .list();
        List<KmsIdentityVO> completeIdentities = identities.stream()
                .filter(this::hasCompleteClientIdentity)
                .collect(Collectors.toList());

        boolean hasServerCert = StringUtils.isNotBlank(kms.getServerCertPem());

        if (completeIdentities.isEmpty()) {
            try {
                KeyToolGrpcClient.HealthResult result = KeyToolGrpcClient.healthKmip(kmsUuid,
                        KmipConfig.fromKms(kms));
                if (shouldDiscardHealthCheckResult(kmsUuid, runningTracker)) {
                    return;
                }
                if (!isKnownKmipHealthStatus(result.getStatusCode())) {
                    handleUnknownHealthCheckResult(kmsUuid, runningTracker,
                            operr("unknown kmip health status without identity: code=%s, error=%s",
                                    result.getStatusCode(), result.getError()));
                    return;
                }
                boolean connected = result.isConnected();
                KmsTrustState trustState = determineTrustStateWithoutIdentity(result);
                if (!resetUnknownFailures(kmsUuid, runningTracker)) {
                    return;
                }
                updateKmsStatus(kmsUuid, connected, trustState, null);
            } catch (Throwable t) {
                handleUnknownHealthCheckResult(kmsUuid, runningTracker,
                        operr("kms health check without identity failed").withException(t.toString()));
            }
            return;
        }

        List<IdentityResult> results = new ArrayList<>();
        KmsIdentityVO currentActive = findCurrentActiveIdentity(kms, completeIdentities);
        if (currentActive != null) {
            IdentityResult activeResult = checkIdentity(kms, currentActive, hasServerCert);
            results.add(activeResult);

            if (shouldDiscardHealthCheckResult(kmsUuid, runningTracker)) {
                return;
            }

            if (activeResult.canBeActiveIdentity()) {
                if (!resetUnknownFailures(kmsUuid, runningTracker)) {
                    return;
                }
                updateKmsStatus(kmsUuid, activeResult.isConnected(), KmsTrustState.MUTUAL_TRUSTED, currentActive.getUuid());
                return;
            }
        }

        List<KmsIdentityVO> candidates = sortNewestFirst(completeIdentities).stream()
                .filter(identity -> currentActive == null || !identity.getUuid().equals(currentActive.getUuid()))
                .collect(Collectors.toList());
        for (KmsIdentityVO identity : candidates) {
            IdentityResult result = checkIdentity(kms, identity, hasServerCert);
            results.add(result);

            if (shouldDiscardHealthCheckResult(kmsUuid, runningTracker)) {
                return;
            }

            if (result.canBeActiveIdentity()) {
                if (!resetUnknownFailures(kmsUuid, runningTracker)) {
                    return;
                }
                updateKmsStatus(kmsUuid, result.isConnected(), KmsTrustState.MUTUAL_TRUSTED, identity.getUuid());
                return;
            }
        }

        for (IdentityResult result : results) {
            if (!result.canBeActiveIdentity() && StringUtils.isNotBlank(result.getError())) {
                logger.debug(String.format("kms identity health check failed[uuid:%s]: %s",
                        result.getIdentity().getUuid(), result.getError()));
            }
        }

        if (!hasKnownIdentityResult(results)) {
            handleUnknownHealthCheckResult(kmsUuid, runningTracker,
                    operr("kms health check finished with only unknown identity results"));
            return;
        }

        if (shouldDiscardHealthCheckResult(kmsUuid, runningTracker)) {
            return;
        }
        KmsIdentityVO active = pickActiveIdentity(results);
        KmsTrustState trustState = calculateTrustState(results, active);
        boolean connected = hasConnectedIdentity(results);
        if (!resetUnknownFailures(kmsUuid, runningTracker)) {
            return;
        }
        updateKmsStatus(kmsUuid, connected, trustState, active == null ? null : active.getUuid());
    }

    private IdentityResult checkIdentity(KmsVO kms, KmsIdentityVO identity, boolean hasServerCert) {
        try {
            KeyToolGrpcClient.HealthResult result = KeyToolGrpcClient.healthKmip(identity.getUuid(),
                    KmipConfig.fromKmsForHealthCheck(kms, identity));
            return new IdentityResult(identity, result, hasServerCert);
        } catch (Throwable t) {
            return new IdentityResult(identity, KeyToolGrpcClient.HealthResult.fail(t.getMessage()), hasServerCert);
        }
    }

    private KmsIdentityVO findCurrentActiveIdentity(KmsVO kms, List<KmsIdentityVO> completeIdentities) {
        if (kms == null || StringUtils.isBlank(kms.getActiveIdentityUuid()) || completeIdentities == null || completeIdentities.isEmpty()) {
            return null;
        }

        return completeIdentities.stream()
                .filter(identity -> kms.getActiveIdentityUuid().equals(identity.getUuid()))
                .findFirst()
                .orElse(null);
    }

    private List<KmsIdentityVO> sortNewestFirst(List<KmsIdentityVO> identities) {
        return identities.stream()
                .sorted(Comparator.comparingLong(this::getIdentityTimestamp)
                        .thenComparing(KmsIdentityVO::getUuid)
                        .reversed())
                .collect(Collectors.toList());
    }

    private boolean hasCompleteClientIdentity(KmsIdentityVO identity) {
        return StringUtils.isNotBlank(identity.getClientCertPem()) && StringUtils.isNotBlank(identity.getClientKeyPem());
    }

    private boolean hasConnectedIdentity(List<IdentityResult> results) {
        return results.stream().anyMatch(IdentityResult::isConnected);
    }

    private boolean hasKnownIdentityResult(List<IdentityResult> results) {
        return results.stream().anyMatch(result -> isKnownKmipHealthStatus(result.getStatusCode()));
    }

    private boolean isKnownKmipHealthStatus(int statusCode) {
        KeyProviderErrors error = KeyProviderErrors.fromKeyToolStatus(statusCode);
        switch (error) {
            case OK:
            case KMIP_CONNECT_FAILED:
            case KMIP_TIMEOUT:
            case KMIP_TLS_HANDSHAKE_FAILED:
            case KMIP_CERT_INVALID:
            case KMIP_OPERATION_FAILED:
            case KMIP_KMS_CERT_UNTRUSTED:
            case KMIP_CLIENT_CERT_UNTRUSTED:
            case KMIP_MUTUAL_CERT_UNTRUSTED:
                return true;
            default:
                return false;
        }
    }

    private boolean resetUnknownFailures(String kmsUuid, Tracker tracker) {
        if (shouldDiscardHealthCheckResult(kmsUuid, tracker)) {
            return false;
        }

        unknownFailureCounts.remove(kmsUuid);
        return true;
    }

    private void handleUnknownHealthCheckResult(String kmsUuid, Tracker tracker, ErrorCode err) {
        if (shouldDiscardHealthCheckResult(kmsUuid, tracker)) {
            return;
        }

        int failures = unknownFailureCounts.merge(kmsUuid, 1, Integer::sum);
        if (failures < MAX_UNKNOWN_FAILURES) {
            logger.warn(String.format("kms health check hit unknown error and keeps previous status[uuid:%s, error:%s, unknownFailureCount=%s/%s]",
                    kmsUuid,
                    err.getReadableDetails(),
                    failures,
                    MAX_UNKNOWN_FAILURES));
            return;
        }

        logger.warn(String.format("kms health check hit unknown error and downgrades to disconnected/untrusted[uuid:%s, error:%s, unknownFailureCount=%s/%s]",
                kmsUuid,
                err.getReadableDetails(),
                failures,
                MAX_UNKNOWN_FAILURES));
        updateKmsStatus(kmsUuid, false, KmsTrustState.MUTUAL_UNTRUSTED, null);
        unknownFailureCounts.remove(kmsUuid);
    }

    private boolean shouldDiscardHealthCheckResult(String kmsUuid, Tracker tracker) {
        if (tracker != null && trackers.get(kmsUuid) == tracker) {
            return false;
        }

        logger.debug(String.format("discard stale kms health check result[uuid:%s]", kmsUuid));
        return true;
    }

    private KmsTrustState calculateTrustState(List<IdentityResult> results, KmsIdentityVO active) {
        if (active != null) {
            return KmsTrustState.MUTUAL_TRUSTED;
        }

        boolean mutualTrusted = false;
        boolean mnTrustsKmsOnly = false;
        boolean kmsTrustsMnOnly = false;
        boolean mutualUntrusted = false;
        for (IdentityResult result : results) {
            KmsTrustState state = result.getTrustState();
            if (state == KmsTrustState.MUTUAL_TRUSTED) {
                mutualTrusted = true;
            } else if (state == KmsTrustState.MN_TRUSTS_KMS_ONLY) {
                mnTrustsKmsOnly = true;
            } else if (state == KmsTrustState.KMS_TRUSTS_MN_ONLY) {
                kmsTrustsMnOnly = true;
            } else if (state == KmsTrustState.MUTUAL_UNTRUSTED) {
                mutualUntrusted = true;
            }
        }

        if (mutualTrusted) {
            return KmsTrustState.MUTUAL_TRUSTED;
        }
        // Mixed trust directions are treated conservatively.
        if (mutualUntrusted || (mnTrustsKmsOnly && kmsTrustsMnOnly)) {
            return KmsTrustState.MUTUAL_UNTRUSTED;
        }
        if (mnTrustsKmsOnly) {
            return KmsTrustState.MN_TRUSTS_KMS_ONLY;
        }
        if (kmsTrustsMnOnly) {
            return KmsTrustState.KMS_TRUSTS_MN_ONLY;
        }
        return KmsTrustState.MUTUAL_UNTRUSTED;
    }

    private KmsTrustState determineTrustStateWithoutIdentity(KeyToolGrpcClient.HealthResult result) {
        if (result == null) {
            return KmsTrustState.MUTUAL_UNTRUSTED;
        }

        boolean serverVerified;
        boolean clientVerified;
        if (result.hasServerCertVerified() || result.hasClientCertVerified()) {
            serverVerified = result.isServerCertVerified();
            clientVerified = result.isClientCertVerified();
        } else {
            // Treat no verify flags as MUTUAL_UNTRUSTED
            serverVerified = false;
            clientVerified = false;
        }

        return resolveTrustState(serverVerified, clientVerified);
    }

    private void updateKmsStatus(String kmsUuid, boolean connected, KmsTrustState trustState, String activeIdentityUuid) {
        KmsTrustState state = trustState == null ? KmsTrustState.MUTUAL_UNTRUSTED : trustState;
        KmsVO current = dbf.findByUuid(kmsUuid, KmsVO.class);
        if (current == null) {
            return;
        }

        boolean changed = current.isConnected() != connected
                || current.getTrustState() != state
                || !StringUtils.equals(current.getActiveIdentityUuid(), activeIdentityUuid);
        if (!changed) {
            logger.debug(String.format("skip kms health status update because nothing changed[uuid:%s]", kmsUuid));
            return;
        }

        logger.debug(String.format("update kms health status[uuid:%s]: connected %s -> %s, trustState %s -> %s, activeIdentityUuid %s -> %s",
                kmsUuid,
                current.isConnected(),
                connected,
                current.getTrustState(),
                state,
                current.getActiveIdentityUuid(),
                activeIdentityUuid));
        SQL.New(KmsVO.class)
                .eq(KmsVO_.uuid, kmsUuid)
                .set(KmsVO_.connected, connected)
                .set(KmsVO_.trustState, state)
                .set(KmsVO_.activeIdentityUuid, activeIdentityUuid)
                .update();
    }

    private KmsIdentityVO pickActiveIdentity(List<IdentityResult> results) {
        return results.stream()
                .filter(IdentityResult::canBeActiveIdentity)
                .map(IdentityResult::getIdentity)
                .max(Comparator.comparingLong(this::getIdentityTimestamp).thenComparing(KmsIdentityVO::getUuid))
                .orElse(null);
    }

    private long getIdentityTimestamp(KmsIdentityVO identity) {
        Timestamp ts = identity.getLastOpDate() != null ? identity.getLastOpDate() : identity.getCreateDate();
        return ts == null ? 0L : ts.getTime();
    }

    private static KmsTrustState resolveTrustState(boolean serverVerified, boolean clientVerified) {
        if (serverVerified && clientVerified) {
            return KmsTrustState.MUTUAL_TRUSTED;
        }
        if (serverVerified) {
            return KmsTrustState.MN_TRUSTS_KMS_ONLY;
        }
        if (clientVerified) {
            return KmsTrustState.KMS_TRUSTS_MN_ONLY;
        }
        return KmsTrustState.MUTUAL_UNTRUSTED;
    }

    private static class IdentityResult {
        private final KmsIdentityVO identity;
        private final KeyToolGrpcClient.HealthResult result;
        private final boolean hasServerCert;

        IdentityResult(KmsIdentityVO identity, KeyToolGrpcClient.HealthResult result, boolean hasServerCert) {
            this.identity = identity;
            this.result = result;
            this.hasServerCert = hasServerCert;
        }

        public KmsIdentityVO getIdentity() {
            return identity;
        }

        public boolean isHealthy() {
            return result != null && result.isHealthy();
        }

        public boolean canBeActiveIdentity() {
            return getTrustState() == KmsTrustState.MUTUAL_TRUSTED;
        }

        public boolean isConnected() {
            return result != null && result.isConnected();
        }

        public String getError() {
            return result == null ? null : result.getError();
        }

        public int getStatusCode() {
            return result == null ? -1 : result.getStatusCode();
        }

        public KmsTrustState getTrustState() {
            if (result == null) {
                return KmsTrustState.MUTUAL_UNTRUSTED;
            }

            boolean serverVerified;
            boolean clientVerified;
            if (result.hasServerCertVerified() || result.hasClientCertVerified()) {
                serverVerified = result.isServerCertVerified();
                clientVerified = result.isClientCertVerified();
            } else {
                // Treat no verify flags as MUTUAL_UNTRUSTED
                serverVerified = false;
                clientVerified = false;
            }

            return resolveTrustState(serverVerified, clientVerified);
        }
    }
}
