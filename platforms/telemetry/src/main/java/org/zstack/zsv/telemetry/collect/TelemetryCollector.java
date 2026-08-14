package org.zstack.zsv.telemetry.collect;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zsv.telemetry.TelemetryConstant;
import org.zstack.zsv.telemetry.TelemetryErrors;
import org.zstack.zsv.telemetry.TelemetryUtils;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;
import org.zstack.header.telemetry.TelemetryLicenseTypeProvider;
import org.zstack.zsv.telemetry.privacy.TelemetryAnonymizer;

import javax.persistence.Tuple;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;

public class TelemetryCollector implements Component {
    private static final CLogger logger = Utils.getLogger(TelemetryCollector.class);
    private static final DateTimeFormatter SNAPSHOT_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private TelemetryAnonymizer anonymizer;
    @Autowired
    private TelemetryLicenseTypeProvider licenseTypeProvider;

    public TelemetryDailyReport collect() {
        try {
            TelemetryDailyReport report = new TelemetryDailyReport();
            report.schemaVersion = TelemetryConstant.SCHEMA_VERSION;
            report.snapshotDate = SNAPSHOT_DATE_FMT.format(Instant.now());
            report.sourceId = TelemetryGlobalConfig.SOURCE_ID.value();
            report.version = dbf.getDbVersion();

            report.system.licenseType = licenseTypeProvider.getLicenseType();

            List<String> mnIds = anonymizeUuids(TelemetryUtils.listManagementNodeUuids());
            List<String> hostIds = anonymizeUuids(listEnabledHostUuids());
            report.compute.mnIds = mnIds;
            report.compute.hostIds = hostIds;
            report.compute.mnCount = mnIds.size();
            report.compute.hostCount = hostIds.size();
            report.compute.vmTotal = Q.New(VmInstanceVO.class).count();
            report.compute.vmOsDistribution = collectVmOsDistribution();

            report.storage.storageTypes = listStorageTypes();
            return report;
        } catch (Throwable t) {
            logger.warn(String.format("collect failed: %s", t.getMessage()), t);
            throw err(TelemetryErrors.TELEMETRY_COLLECT_FAILED, "failed to collect telemetry data: %s", t.getMessage())
                    .toException();
        }
    }

    /**
     * Mini payload for check-update (no consent gate; caller decides). Does not touch local reports/pending.
     */
    public TelemetryCheckUpdateReport collectCheckUpdateReport() {
        try {
            TelemetryCheckUpdateReport report = new TelemetryCheckUpdateReport();
            report.sourceId = TelemetryGlobalConfig.SOURCE_ID.value();
            report.licenseType = licenseTypeProvider.getLicenseType();
            report.version = dbf.getDbVersion();
            report.mnIds = anonymizeUuids(TelemetryUtils.listManagementNodeUuids());
            report.hostIds = anonymizeUuids(listEnabledHostUuids());
            report.requestType = TelemetryConstant.REQUEST_TYPE_CHECK_UPDATE;
            return report;
        } catch (Throwable t) {
            logger.warn(String.format("collect check-update report failed: %s", t.getMessage()), t);
            throw err(TelemetryErrors.TELEMETRY_COLLECT_FAILED,
                    "failed to collect check-update data: %s", t.getMessage()).toException();
        }
    }

    private List<String> listEnabledHostUuids() {
        List<String> uuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.state, HostState.Enabled)
                .listValues();
        return uuids == null ? new ArrayList<>() : uuids;
    }

    private List<String> listStorageTypes() {
        List<String> types = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .groupBy(PrimaryStorageVO_.type)
                .listValues();
        if (types == null) {
            return new ArrayList<>();
        }
        return types.stream().filter(t -> t != null && !t.isEmpty()).collect(Collectors.toList());
    }

    private Map<String, Long> collectVmOsDistribution() {
        Map<String, Long> distribution = new LinkedHashMap<>();
        List<Tuple> rows = SQL.New(
                "select vm.guestOsType, count(vm.uuid) from VmInstanceVO vm group by vm.guestOsType",
                Tuple.class
        ).list();
        if (rows == null) {
            return distribution;
        }

        for (Tuple row : rows) {
            String guestOs = row.get(0, String.class);
            Long count = row.get(1, Long.class);
            String key = (guestOs == null || guestOs.trim().isEmpty()) ? TelemetryConstant.OS_UNKNOWN : guestOs;
            distribution.merge(key, count == null ? 0L : count, Long::sum);
        }
        return distribution;
    }

    private List<String> anonymizeUuids(List<String> uuids) {
        return uuids.stream().map(anonymizer::anonymize).collect(Collectors.toList());
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
