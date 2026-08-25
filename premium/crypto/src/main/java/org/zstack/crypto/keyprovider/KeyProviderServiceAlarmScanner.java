package org.zstack.crypto.keyprovider;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.header.Component;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO_;
import org.zstack.header.keyprovider.KeyProviderCanonicalEvents;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;
import org.zstack.header.keyprovider.KmsTrustState;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class KeyProviderServiceAlarmScanner implements Component, ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener {
    private static final CLogger logger = Utils.getLogger(KeyProviderServiceAlarmScanner.class);
    private static final long SCAN_INTERVAL_SECONDS = 120L;
    private static final String SCANNER_NAME = "keyProviderService";
    private static final String DEFAULT_ALARM_TYPE = "DefaultServiceUnavailable";
    private static final String RESOURCE_ALARM_TYPE = "ResourceServiceUnavailable";
    private Scanner scanner;

    @Autowired
    private EventFacade evtf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ScannerAlarmStateStore alarmStateStore;

    @Override
    public void managementNodeReady() {
        restartServiceAlarmScanner();
    }

    @Override
    @AsyncThread
    public void nodeJoin(ManagementNodeInventory inv) {
        restartServiceAlarmScanner();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        restartServiceAlarmScanner();
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
        stopScanner();
        return true;
    }

    private void restartServiceAlarmScanner() {
        stopScanner();
        hydrateAlarmStates();
        recoverUnmanagedAlarms();
        startScanner();
    }

    private void hydrateAlarmStates() {
        alarmStateStore.hydrate(SCANNER_NAME, DEFAULT_ALARM_TYPE);
        alarmStateStore.hydrate(SCANNER_NAME, RESOURCE_ALARM_TYPE);
    }

    private void startScanner() {
        scanner = new Scanner();
        scanner.start();
    }

    private void stopScanner() {
        if (scanner != null) {
            scanner.cancel();
            scanner = null;
        }
    }

    void scanAll() {
        scanDefaultProviderAlarm();
        scanResourceProviderAlarm();
    }

    private void scanDefaultProviderAlarm() {
        String defaultUuid = KeyProviderHealthScope.getDefaultProviderUuid();
        Set<String> scanned = new HashSet<>();
        if (StringUtils.isNotBlank(defaultUuid) && destMaker.isManagedByUs(defaultUuid)
                && KeyProviderHealthScope.hasResourceRef(defaultUuid)) {
            ProviderHealth health = findProviderHealthOrMissing(defaultUuid);
            scanned.add(defaultUuid);
            handleServiceAlarm(DEFAULT_ALARM_TYPE, defaultUuid, health, true);
        }
        recoverActiveAlarmsNoLongerScanned(DEFAULT_ALARM_TYPE, scanned, true);
        alarmStateStore.evictInactiveNotIn(SCANNER_NAME, DEFAULT_ALARM_TYPE, scanned);
    }

    private void scanResourceProviderAlarm() {
        String defaultUuid = KeyProviderHealthScope.getDefaultProviderUuid();
        Set<String> providerUuids = new HashSet<>();
        Set<String> orphanProviderNames = new HashSet<>();
        collectResourceProviderTargets(providerUuids, orphanProviderNames);

        Set<String> scannedKeys = new HashSet<>();
        for (String providerUuid : providerUuids.stream()
                .filter(uuid -> !StringUtils.equals(uuid, defaultUuid))
                .filter(uuid -> destMaker.isManagedByUs(uuid))
                .collect(Collectors.toSet())) {
            ProviderHealth health = findProviderHealth(providerUuid);
            if (health == null) {
                health = ProviderHealth.missing(providerUuid, findResourceRefProviderName(providerUuid));
            }
            handleServiceAlarm(RESOURCE_ALARM_TYPE, providerUuid, health, false);
            scannedKeys.add(providerUuid);
        }
        for (String providerName : orphanProviderNames) {
            handleServiceAlarm(RESOURCE_ALARM_TYPE, providerName,
                    ProviderHealth.missingWithNameAsId(providerName), false);
            scannedKeys.add(providerName);
        }
        recoverActiveAlarmsNoLongerScanned(RESOURCE_ALARM_TYPE, scannedKeys, false);
        alarmStateStore.evictInactiveNotIn(SCANNER_NAME, RESOURCE_ALARM_TYPE, scannedKeys);
    }

    private void collectResourceProviderTargets(Set<String> providerUuids, Set<String> orphanProviderNames) {
        List<String> refProviderUuids = Q.New(EncryptedResourceKeyRefVO.class)
                .select(EncryptedResourceKeyRefVO_.providerUuid)
                .notNull(EncryptedResourceKeyRefVO_.providerUuid)
                .listValues()
                .stream()
                .map(String::valueOf)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        providerUuids.addAll(refProviderUuids);

        List<String> nameOnlyRefs = Q.New(EncryptedResourceKeyRefVO.class)
                .select(EncryptedResourceKeyRefVO_.providerName)
                .isNull(EncryptedResourceKeyRefVO_.providerUuid)
                .notNull(EncryptedResourceKeyRefVO_.providerName)
                .listValues()
                .stream()
                .map(String::valueOf)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        for (String providerName : nameOnlyRefs) {
            String resolvedUuid = Q.New(KeyProviderVO.class)
                    .eq(KeyProviderVO_.name, providerName)
                    .select(KeyProviderVO_.uuid)
                    .findValue();
            if (StringUtils.isNotBlank(resolvedUuid)) {
                providerUuids.add(resolvedUuid);
            } else {
                orphanProviderNames.add(providerName);
            }
        }
    }

    private String findResourceRefProviderName(String providerUuid) {
        String providerName = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.providerUuid, providerUuid)
                .select(EncryptedResourceKeyRefVO_.providerName)
                .findValue();
        return StringUtils.isBlank(providerName) ? providerUuid : providerName;
    }

    private void recoverUnmanagedAlarms() {
        recoverAlarmsNotOurs(DEFAULT_ALARM_TYPE, true);
        recoverAlarmsNotOurs(RESOURCE_ALARM_TYPE, false);
    }

    private void recoverAlarmsNotOurs(String alarmType, boolean defaultAlarm) {
        for (String alarmKey : alarmStateStore.activeAlarmKeys(SCANNER_NAME, alarmType)) {
            if (dbf.findByUuid(alarmKey, KeyProviderVO.class) == null) {
                continue;
            }
            if (!destMaker.isManagedByUs(alarmKey)) {
                recoverServiceAlarm(alarmType, alarmKey, findProviderHealthOrMissing(alarmKey), defaultAlarm);
            }
        }
    }

    private ProviderHealth findProviderHealth(String providerUuid) {
        if (StringUtils.isBlank(providerUuid)) {
            return null;
        }

        KmsVO kms = dbf.findByUuid(providerUuid, KmsVO.class);
        if (kms != null) {
            return ProviderHealth.fromKms(kms);
        }

        NkpVO nkp = dbf.findByUuid(providerUuid, NkpVO.class);
        if (nkp != null) {
            return ProviderHealth.fromNkp(nkp);
        }

        return null;
    }

    private ProviderHealth findProviderHealthOrMissing(String providerUuid) {
        ProviderHealth health = findProviderHealth(providerUuid);
        return health == null ? ProviderHealth.missing(providerUuid, findResourceRefProviderName(providerUuid)) : health;
    }

    private void handleServiceAlarm(String alarmType, String providerUuid, ProviderHealth health, boolean defaultAlarm) {
        ScannerAlarmStateStore.AlarmState state = alarmStateStore.getAlarmState(SCANNER_NAME, alarmType, providerUuid);
        if (health.available) {
            if (state == ScannerAlarmStateStore.AlarmState.ACTIVE) {
                recoverServiceAlarm(alarmType, providerUuid, health, defaultAlarm);
            }
        } else if (state != ScannerAlarmStateStore.AlarmState.ACTIVE) {
            reportServiceAlarm(alarmType, providerUuid, health, defaultAlarm);
        }
    }

    private void reportServiceAlarm(String alarmType, String providerUuid, ProviderHealth health, boolean defaultAlarm) {
        if (defaultAlarm) {
            fireDefaultServiceUnavailable(health);
        } else {
            fireResourceServiceUnavailable(health);
        }
        alarmStateStore.markActive(SCANNER_NAME, alarmType, providerUuid);
    }

    private void recoverServiceAlarm(String alarmType, String providerUuid, ProviderHealth health, boolean defaultAlarm) {
        if (defaultAlarm) {
            fireDefaultServiceRecovered(health);
        } else {
            fireResourceServiceRecovered(health);
        }
        alarmStateStore.markInactive(SCANNER_NAME, alarmType, providerUuid);
    }

    private void recoverActiveAlarmsNoLongerScanned(String alarmType, Set<String> scannedProviderUuids, boolean defaultAlarm) {
        for (String alarmKey : alarmStateStore.activeAlarmKeys(SCANNER_NAME, alarmType)) {
            if (!scannedProviderUuids.contains(alarmKey)) {
                recoverServiceAlarm(alarmType, alarmKey, healthForRecover(alarmKey), defaultAlarm);
            }
        }
    }

    private ProviderHealth healthForRecover(String alarmKey) {
        if (dbf.findByUuid(alarmKey, KeyProviderVO.class) != null) {
            return findProviderHealthOrMissing(alarmKey);
        }
        String resolvedUuid = Q.New(KeyProviderVO.class)
                .eq(KeyProviderVO_.name, alarmKey)
                .select(KeyProviderVO_.uuid)
                .findValue();
        if (StringUtils.isNotBlank(resolvedUuid)) {
            ProviderHealth health = findProviderHealth(resolvedUuid);
            if (health != null) {
                return ProviderHealth.recoveredWithNameAsId(alarmKey, health);
            }
        }
        return ProviderHealth.missingWithNameAsId(alarmKey);
    }

    private void fireDefaultServiceUnavailable(ProviderHealth health) {
        KeyProviderCanonicalEvents.KeyProviderServiceUnavailableData data =
                new KeyProviderCanonicalEvents.KeyProviderServiceUnavailableData();
        fillKeyProviderServiceData(health, data);
        logger.info(String.format(
                "key provider default service unavailable alarm fired, providerUuid=%s, providerName=%s, type=%s",
                health.uuid, health.name, health.type));
        evtf.fire(KeyProviderCanonicalEvents.KEY_PROVIDER_DEFAULT_SERVICE_UNAVAILABLE_PATH, data);
    }

    private void fireDefaultServiceRecovered(ProviderHealth health) {
        KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData data =
                new KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData();
        fillKeyProviderServiceData(health, data);
        logger.info(String.format(
                "key provider default service recovered alarm fired, providerUuid=%s, providerName=%s, type=%s",
                health.uuid, health.name, health.type));
        evtf.fire(KeyProviderCanonicalEvents.KEY_PROVIDER_DEFAULT_SERVICE_RECOVERED_PATH, data);
    }

    private void fireResourceServiceUnavailable(ProviderHealth health) {
        KeyProviderCanonicalEvents.KeyProviderServiceUnavailableData data =
                new KeyProviderCanonicalEvents.KeyProviderServiceUnavailableData();
        fillKeyProviderServiceData(health, data);
        logger.info(String.format(
                "key provider resource service unavailable alarm fired, providerUuid=%s, providerName=%s, type=%s",
                health.uuid, health.name, health.type));
        evtf.fire(KeyProviderCanonicalEvents.KEY_PROVIDER_RESOURCE_SERVICE_UNAVAILABLE_PATH, data);
    }

    private void fireResourceServiceRecovered(ProviderHealth health) {
        KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData data =
                new KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData();
        fillKeyProviderServiceData(health, data);
        logger.info(String.format(
                "key provider resource service recovered alarm fired, providerUuid=%s, providerName=%s, type=%s",
                health.uuid, health.name, health.type));
        evtf.fire(KeyProviderCanonicalEvents.KEY_PROVIDER_RESOURCE_SERVICE_RECOVERED_PATH, data);
    }

    private void fillKeyProviderServiceData(ProviderHealth health, KeyProviderCanonicalEvents.KeyProviderServiceUnavailableData data) {
        data.setKeyProviderUuid(health.uuid);
        data.setKeyProviderName(health.name);
        data.setKeyProviderType(health.type);
        data.setStatusCode(null);
        data.setStatusMessage(health.statusMessage);
        data.setDefaultProvider(String.valueOf(KeyProviderHealthScope.isDefaultProvider(health.uuid)));
    }

    private void fillKeyProviderServiceData(ProviderHealth health, KeyProviderCanonicalEvents.KeyProviderServiceRecoveredData data) {
        data.setKeyProviderUuid(health.uuid);
        data.setKeyProviderName(health.name);
        data.setKeyProviderType(health.type);
        data.setStatusCode(null);
        data.setStatusMessage(health.statusMessage);
        data.setDefaultProvider(String.valueOf(KeyProviderHealthScope.isDefaultProvider(health.uuid)));
    }

    private static class ProviderHealth {
        private final String uuid;
        private final String name;
        private final String type;
        private final boolean available;
        private final String statusMessage;

        private ProviderHealth(String uuid, String name, String type, boolean available, String statusMessage) {
            this.uuid = uuid;
            this.name = name;
            this.type = type;
            this.available = available;
            this.statusMessage = statusMessage;
        }

        private static ProviderHealth fromKms(KmsVO kms) {
            boolean available = kms.isConnected() && kms.getTrustState() == KmsTrustState.MUTUAL_TRUSTED;
            return new ProviderHealth(kms.getUuid(), kms.getName(),
                    kms.getType() == null ? KeyProviderType.KMS.toString() : kms.getType().toString(),
                    available, kmsStatusMessage(kms, available));
        }

        private static ProviderHealth fromNkp(NkpVO nkp) {
            boolean available = nkp.isConnected();
            return new ProviderHealth(nkp.getUuid(), nkp.getName(),
                    nkp.getType() == null ? KeyProviderType.NKP.toString() : nkp.getType().toString(),
                    available, available ? null : "key provider is disconnected");
        }

        private static ProviderHealth missing(String uuid, String name) {
            return new ProviderHealth(uuid, name, "KeyProvider", false, "associated key provider does not exist");
        }

        private static ProviderHealth missingWithNameAsId(String name) {
            return missing(name, name);
        }

        private static ProviderHealth recoveredWithNameAsId(String nameAsId, ProviderHealth health) {
            return new ProviderHealth(nameAsId, health.name, health.type, true, null);
        }

        private static String kmsStatusMessage(KmsVO kms, boolean available) {
            if (available) {
                return null;
            }
            if (!kms.isConnected()) {
                return "key provider is disconnected";
            }
            return "key provider mutual trust is not established";
        }
    }

    private class Scanner extends AsyncTimer {
        Scanner() {
            super(TimeUnit.SECONDS, SCAN_INTERVAL_SECONDS);
            __name__ = "key-provider-service-alarm-scanner";
        }

        @Override
        protected void execute() {
            try {
                scanAll();
            } catch (Exception e) {
                logger.warn("key provider service alarm scan failed", e);
            } finally {
                continueToRunThisTimer();
            }
        }
    }
}
