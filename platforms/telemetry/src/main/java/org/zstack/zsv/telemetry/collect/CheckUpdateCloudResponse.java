package org.zstack.zsv.telemetry.collect;

import com.google.gson.annotations.SerializedName;

/**
 * Check-update response JSON from Telemetry Cloud (snake_case wire keys).
 * Same style as {@link TelemetryDailyReport} / {@link TelemetryCheckUpdateReport}: not VO/Message.
 */
public class CheckUpdateCloudResponse {
    @SerializedName("version")
    public String version;
    @SerializedName("release_notes_zh")
    public String releaseNotesZh;
    @SerializedName("release_notes_en")
    public String releaseNotesEn;
}
