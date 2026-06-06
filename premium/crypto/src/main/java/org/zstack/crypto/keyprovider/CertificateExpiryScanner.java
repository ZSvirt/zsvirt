package org.zstack.crypto.keyprovider;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.Q;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.header.Component;
import org.zstack.header.keyprovider.KeyProviderCanonicalEvents;
import org.zstack.header.keyprovider.KeyProviderGlobalConfig;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KmsIdentityVO;
import org.zstack.header.keyprovider.KmsIdentityVO_;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.pki.HostCertificateVO;
import org.zstack.header.pki.HostCertificateVO_;
import org.zstack.header.pki.HostPkiConstant;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CertificateExpiryScanner implements Component, ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener {
    private static final CLogger logger = Utils.getLogger(CertificateExpiryScanner.class);
    static final long CERTIFICATE_EXPIRING_DAYS = 30L;
    private static final long SCAN_INTERVAL_HOURS = 24L;

    private final Map<String, CertificateReport> certificateExpiringReports = new ConcurrentHashMap<>();
    private final Map<String, Long> lastReportEpochDay = new ConcurrentHashMap<>();
    private DailyScanner scanner;

    @Autowired
    private EventFacade evtf;
    @Autowired
    private ResourceDestinationMaker destMaker;

    @Override
    public void managementNodeReady() {
        restartCertificateScanner();
    }

    @Override
    @AsyncThread
    public void nodeJoin(ManagementNodeInventory inv) {
        restartCertificateScanner();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        restartCertificateScanner();
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
        certificateExpiringReports.clear();
        lastReportEpochDay.clear();
        return true;
    }

    private void restartCertificateScanner() {
        stopScanner();
        recoverUnmanagedAlarms();
        startScanner();
    }

    private void startScanner() {
        scanner = new DailyScanner();
        // Run once when the timer starts, then every SCAN_INTERVAL_HOURS (avoid missing scans
        // when MN restarts more often than the interval).
        scanner.startRightNow();
    }

    private void recoverUnmanagedAlarms() {
        certificateExpiringReports.keySet().removeIf(reportKey -> {
            CertificateReport report = certificateExpiringReports.get(reportKey);
            if (report == null || destMaker.isManagedByUs(report.resourceUuid)) {
                return false;
            }
            lastReportEpochDay.remove(reportKey);
            fireCertificateRecovered(report);
            return true;
        });
    }

    private void stopScanner() {
        if (scanner != null) {
            scanner.cancel();
            scanner = null;
        }
    }

    void scanAll() {
        Set<String> scannedReportKeys = new HashSet<>();
        List<KmsVO> kmsList = Q.New(KmsVO.class).list();
        for (KmsVO kms : kmsList) {
            if (kms == null || !destMaker.isManagedByUs(kms.getUuid())) {
                continue;
            }
            fireCertificateExpiringIfNeeded(buildKmsReportKey(kms.getUuid(), "server"),
                    kms.getUuid(), kms.getName(),
                    kms.getType() == null ? KeyProviderType.KMS.toString() : kms.getType().toString(),
                    "server", kms.getServerCertExpiredDate(), isDefaultProvider(kms.getUuid()),
                    scannedReportKeys);

            List<KmsIdentityVO> identities = Q.New(KmsIdentityVO.class)
                    .eq(KmsIdentityVO_.kmsUuid, kms.getUuid())
                    .list();
            for (KmsIdentityVO identity : identities) {
                if (identity == null || StringUtils.isBlank(identity.getClientCertPem())) {
                    continue;
                }
                String certType = identity.getIdentityType() == null
                        ? "client"
                        : "client:" + identity.getIdentityType();
                fireCertificateExpiringIfNeeded(buildKmsReportKey(kms.getUuid(), "client:" + identity.getUuid()),
                        kms.getUuid(), kms.getName(),
                        kms.getType() == null ? KeyProviderType.KMS.toString() : kms.getType().toString(),
                        certType, identity.getCertExpiredDate(), isDefaultProvider(kms.getUuid()),
                        scannedReportKeys);
            }
        }

        List<HostCertificateVO> hostCerts = Q.New(HostCertificateVO.class)
                .eq(HostCertificateVO_.usage, HostPkiConstant.USAGE_MIGRATION)
                .list();
        for (HostCertificateVO cert : hostCerts) {
            if (cert == null || StringUtils.isBlank(cert.getHostUuid())
                    || !destMaker.isManagedByUs(cert.getHostUuid())) {
                continue;
            }
            String hostName = Q.New(ResourceVO.class)
                    .select(ResourceVO_.resourceName)
                    .eq(ResourceVO_.uuid, cert.getHostUuid())
                    .findValue();
            if (StringUtils.isBlank(hostName)) {
                hostName = cert.getHostUuid();
            }
            fireCertificateExpiringIfNeeded(buildHostReportKey(cert.getHostUuid(), cert.getUuid()),
                    cert.getHostUuid(), hostName,
                    "HostMigration", "host:migration", cert.getNotAfter(), "false",
                    scannedReportKeys);
        }
        recoverMissingReports(scannedReportKeys);
    }

    static String buildKmsReportKey(String kmsUuid, String suffix) {
        return kmsUuid + ":" + suffix;
    }

    static String buildHostReportKey(String hostUuid, String certUuid) {
        return hostUuid + ":host:migration:" + certUuid;
    }

    void fireCertificateExpiringIfNeeded(String reportKey, String resourceUuid, String resourceName,
                                         String resourceType, String certificateType,
                                         Timestamp expiredDate, String defaultProvider) {
        fireCertificateExpiringIfNeeded(reportKey, resourceUuid, resourceName, resourceType, certificateType,
                expiredDate, defaultProvider, new HashSet<>());
    }

    private void fireCertificateExpiringIfNeeded(String reportKey, String resourceUuid, String resourceName,
                                                 String resourceType, String certificateType,
                                                 Timestamp expiredDate, String defaultProvider,
                                                 Set<String> scannedReportKeys) {
        scannedReportKeys.add(reportKey);
        if (expiredDate == null) {
            recoverReport(reportKey);
            return;
        }

        long daysLeft = computeDaysLeft(expiredDate);
        CertificateReport report = new CertificateReport(reportKey, resourceUuid, resourceName, resourceType,
                certificateType, expiredDate, daysLeft, defaultProvider);
        if (daysLeft > CERTIFICATE_EXPIRING_DAYS) {
            recoverReport(reportKey);
            return;
        }

        CertificateReport oldReport = certificateExpiringReports.get(reportKey);
        if (oldReport != null && (
                !Objects.equals(oldReport.expiredDate, expiredDate)
                        || !Objects.equals(oldReport.certificateType, certificateType))) {
            fireCertificateRecovered(oldReport);
            oldReport = null;
            lastReportEpochDay.remove(reportKey);
        }

        long today = currentEpochDay();
        Long lastReportDay = lastReportEpochDay.get(reportKey);
        if (lastReportDay != null && lastReportDay >= today) {
            certificateExpiringReports.put(reportKey, report);
            return;
        }

        certificateExpiringReports.put(reportKey, report);
        lastReportEpochDay.put(reportKey, today);
        fireCertificateExpiring(report);
    }

    private void fireCertificateExpiring(CertificateReport report) {
        KeyProviderCanonicalEvents.KeyProviderCertificateExpiringData data =
                new KeyProviderCanonicalEvents.KeyProviderCertificateExpiringData();
        fillCertificateData(data, report);
        logger.info(String.format(
                "key provider certificate expiring alarm fired, resourceUuid=%s, resourceName=%s, certificateType=%s, daysLeft=%d",
                report.resourceUuid, report.resourceName, report.certificateType, report.daysLeft));
        evtf.fire(KeyProviderCanonicalEvents.KEY_PROVIDER_CERTIFICATE_EXPIRING_PATH, data);
    }

    private void fireCertificateRecovered(CertificateReport report) {
        KeyProviderCanonicalEvents.KeyProviderCertificateRecoveredData data =
                new KeyProviderCanonicalEvents.KeyProviderCertificateRecoveredData();
        fillCertificateData(data, report);
        logger.info(String.format(
                "key provider certificate expiring recovered alarm fired, resourceUuid=%s, resourceName=%s, certificateType=%s, daysLeft=%d",
                report.resourceUuid, report.resourceName, report.certificateType, report.daysLeft));
        evtf.fire(KeyProviderCanonicalEvents.KEY_PROVIDER_CERTIFICATE_RECOVERED_PATH, data);
    }

    private void fillCertificateData(KeyProviderCanonicalEvents.KeyProviderCertificateExpiringData data,
                                     CertificateReport report) {
        data.setKeyProviderUuid(report.resourceUuid);
        data.setKeyProviderName(report.resourceName);
        data.setKeyProviderType(report.resourceType);
        data.setCertificateType(report.certificateType);
        data.setExpiredDate(report.expiredDate == null ? null : report.expiredDate.toString());
        data.setDaysLeft(String.valueOf(report.daysLeft));
        data.setReportDate(currentReportDateUtc());
        data.setDefaultProvider(report.defaultProvider);
    }

    private void fillCertificateData(KeyProviderCanonicalEvents.KeyProviderCertificateRecoveredData data,
                                     CertificateReport report) {
        data.setKeyProviderUuid(report.resourceUuid);
        data.setKeyProviderName(report.resourceName);
        data.setKeyProviderType(report.resourceType);
        data.setCertificateType(report.certificateType);
        data.setExpiredDate(report.expiredDate == null ? null : report.expiredDate.toString());
        data.setDaysLeft(String.valueOf(report.daysLeft));
        data.setReportDate(currentReportDateUtc());
        data.setDefaultProvider(report.defaultProvider);
    }

    private void recoverReport(String reportKey) {
        CertificateReport report = certificateExpiringReports.remove(reportKey);
        lastReportEpochDay.remove(reportKey);
        if (report != null) {
            fireCertificateRecovered(report);
        }
    }

    private void recoverMissingReports(Set<String> scannedReportKeys) {
        certificateExpiringReports.keySet().removeIf(reportKey -> {
            if (scannedReportKeys.contains(reportKey)) {
                return false;
            }
            CertificateReport report = certificateExpiringReports.get(reportKey);
            lastReportEpochDay.remove(reportKey);
            if (report != null) {
                fireCertificateRecovered(report);
            }
            return true;
        });
    }

    static long computeDaysLeft(Timestamp expiredDate) {
        long millisLeft = expiredDate.getTime() - System.currentTimeMillis();
        if (millisLeft <= 0) {
            return 0L;
        }
        return (long) Math.ceil(millisLeft / (double) TimeUnit.DAYS.toMillis(1));
    }

    static long currentEpochDay() {
        return Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay();
    }

    static String currentReportDateUtc() {
        return Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneOffset.UTC).toLocalDate().toString();
    }

    private String isDefaultProvider(String providerUuid) {
        return String.valueOf(StringUtils.equals(
                KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.value(String.class), providerUuid));
    }

    private static class CertificateReport {
        private final String reportKey;
        private final String resourceUuid;
        private final String resourceName;
        private final String resourceType;
        private final String certificateType;
        private final Timestamp expiredDate;
        private final long daysLeft;
        private final String defaultProvider;

        CertificateReport(String reportKey, String resourceUuid, String resourceName, String resourceType,
                          String certificateType, Timestamp expiredDate, long daysLeft, String defaultProvider) {
            this.reportKey = reportKey;
            this.resourceUuid = resourceUuid;
            this.resourceName = resourceName;
            this.resourceType = resourceType;
            this.certificateType = certificateType;
            this.expiredDate = expiredDate;
            this.daysLeft = daysLeft;
            this.defaultProvider = defaultProvider;
        }
    }

    private class DailyScanner extends AsyncTimer {
        DailyScanner() {
            super(TimeUnit.HOURS, SCAN_INTERVAL_HOURS);
            __name__ = "certificate-expiry-scanner";
        }

        @Override
        protected void execute() {
            try {
                scanAll();
            } catch (Exception e) {
                logger.warn("certificate expiry scan failed", e);
            } finally {
                continueToRunThisTimer();
            }
        }
    }
}
