package org.zstack.crypto.keyprovider.nkp;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.crypto.keyprovider.KeyToolGrpcClient;
import org.zstack.header.Component;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderUtils;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.header.keyprovider.NkpVO_;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NkpHealthTracker implements Component, ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener {
    private static final CLogger logger = Utils.getLogger(NkpHealthTracker.class);
    private static final long DEFAULT_HEALTH_INTERVAL_SECONDS = 60L;

    private final Map<String, Tracker> trackers = new ConcurrentHashMap<>();
    private final Object trackerLock = new Object();

    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void managementNodeReady() {
        reScanNkp();
    }

    @Override
    @AsyncThread
    public void nodeJoin(ManagementNodeInventory inv) {
        reScanNkp();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        reScanNkp();
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
        }
        return true;
    }

    public void trackNkp(String nkpUuid) {
        if (StringUtils.isBlank(nkpUuid) || !destMaker.isManagedByUs(nkpUuid)) {
            return;
        }

        Tracker tracker;
        Tracker existing;
        synchronized (trackerLock) {
            existing = trackers.get(nkpUuid);
            if (existing != null) {
                existing.cancel();
            }

            tracker = new Tracker(nkpUuid);
            trackers.put(nkpUuid, tracker);
        }
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            tracker.start();
        } else {
            tracker.startRightNow();
        }
    }

    public void untrackNkp(String nkpUuid) {
        Tracker tracker;
        synchronized (trackerLock) {
            tracker = trackers.remove(nkpUuid);
        }
        if (tracker != null) {
            tracker.cancel();
        }
    }

    public void triggerHealthCheck(String nkpUuid) {
        trackNkp(nkpUuid);
    }

    private void reScanNkp() {
        synchronized (trackerLock) {
            trackers.values().forEach(Tracker::cancel);
            trackers.clear();
        }

        Set<String> toTrack = Q.New(NkpVO.class)
                .select(NkpVO_.uuid)
                .listValues()
                .stream()
                .map(String::valueOf)
                .filter(uuid -> destMaker.isManagedByUs(uuid))
                .collect(Collectors.toSet());
        toTrack.forEach(this::trackNkp);
    }

    private class Tracker extends AsyncTimer {
        private final String nkpUuid;

        Tracker(String nkpUuid) {
            super(TimeUnit.SECONDS, DEFAULT_HEALTH_INTERVAL_SECONDS);
            this.nkpUuid = nkpUuid;
            __name__ = String.format("nkp-health-tracker-%s", nkpUuid);
        }

        @Override
        protected void execute() {
            checkHealth();
        }

        private void checkHealth() {
            thdf.chainSubmit(new org.zstack.core.thread.ChainTask(new AsyncBackup() {
            }) {
                @Override
                public String getSyncSignature() {
                    return KeyProviderUtils.getSyncSignature(KeyProviderType.NKP, nkpUuid);
                }

                @Override
                public void run(org.zstack.core.thread.SyncTaskChain chain) {
                    try {
                        doHealthCheck(nkpUuid);
                    } finally {
                        chain.next();
                        Tracker.this.continueToRunThisTimer();
                    }
                }

                @Override
                public String getName() {
                    return String.format("nkp-health-check-%s", nkpUuid);
                }
            });
        }
    }

    private void doHealthCheck(String nkpUuid) {
        NkpVO nkp = dbf.findByUuid(nkpUuid, NkpVO.class);
        if (nkp == null) {
            untrackNkp(nkpUuid);
            return;
        }

        KeyToolGrpcClient.HealthResult result;
        try {
            result = KeyToolGrpcClient.healthNkp(nkpUuid, nkp.getName(), nkp.getUuid());
        } catch (Exception e) {
            logger.warn(String.format("nkp health check exception[uuid:%s]", nkpUuid), e);
            result = KeyToolGrpcClient.HealthResult.fail(e.getMessage());
        }

        if (!result.isHealthy() && StringUtils.isNotBlank(result.getError())) {
            logger.debug(String.format("nkp health check failed[uuid:%s]: %s", nkpUuid, result.getError()));
        }

        boolean connected = result.isHealthy();
        if (nkp.isConnected() != connected) {
            nkp.setConnected(connected);
            dbf.update(nkp);
        }
    }
}
