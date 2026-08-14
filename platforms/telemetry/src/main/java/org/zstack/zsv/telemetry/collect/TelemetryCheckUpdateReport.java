package org.zstack.zsv.telemetry.collect;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Check-update request JSON (mini report). Same style as {@link TelemetryDailyReport}:
 * snake_case wire keys via {@link SerializedName}; not VO/Message — no getter/setter.
 */
public class TelemetryCheckUpdateReport {
    @SerializedName("source_id")
    public String sourceId;
    @SerializedName("license_type")
    public String licenseType;
    @SerializedName("version")
    public String version;
    @SerializedName("mn_ids")
    public List<String> mnIds = new ArrayList<>();
    @SerializedName("host_ids")
    public List<String> hostIds = new ArrayList<>();
    @SerializedName("request_type")
    public String requestType;
}
