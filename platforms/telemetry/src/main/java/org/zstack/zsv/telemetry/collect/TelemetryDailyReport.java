package org.zstack.zsv.telemetry.collect;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily Report JSON model (not VO/Inventory/Message/Reply — no getter/setter required).
 * <p>
 * All schema object keys transmitted to disk / Cloud MUST be lowercase snake_case
 * (e.g. {@code schema_version}, {@code license_type}). Java fields may stay camelCase;
 * serialization relies on {@link SerializedName} for every property.
 * Map value keys under {@code vm_os_distribution} follow guest OS raw strings (see FunctionSpec D9).
 */
public class TelemetryDailyReport {
    @SerializedName("schema_version")
    public String schemaVersion;
    @SerializedName("snapshot_date")
    public String snapshotDate;
    @SerializedName("source_id")
    public String sourceId;
    @SerializedName("version")
    public String version;
    @SerializedName("system")
    public SystemSection system = new SystemSection();
    @SerializedName("compute")
    public ComputeSection compute = new ComputeSection();
    @SerializedName("storage")
    public StorageSection storage = new StorageSection();

    public static class SystemSection {
        @SerializedName("license_type")
        public String licenseType;
    }

    public static class ComputeSection {
        @SerializedName("mn_ids")
        public List<String> mnIds = new ArrayList<>();
        @SerializedName("host_ids")
        public List<String> hostIds = new ArrayList<>();
        @SerializedName("mn_count")
        public long mnCount;
        @SerializedName("host_count")
        public long hostCount;
        @SerializedName("vm_total")
        public long vmTotal;
        @SerializedName("vm_os_distribution")
        public Map<String, Long> vmOsDistribution = new LinkedHashMap<>();
    }

    public static class StorageSection {
        @SerializedName("storage_types")
        public List<String> storageTypes = new ArrayList<>();
    }
}
