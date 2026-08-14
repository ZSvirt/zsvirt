package org.zstack.zsv.telemetry;

public class TelemetryConstant {
    private TelemetryConstant() {
    }

    public static final String SERVICE_ID = "telemetry";

    public static final String SCHEMA_VERSION = "1.0";

    public static final String CONSENT_NOT_GRANTED = "None";

    public static final String CONSENT_GRANTED_AT_PATTERN = "None|^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";

    /** Allow unset placeholder or HTTPS base URL only. */
    public static final String CLOUD_BASE_URL_PATTERN = "None|^https://.+$";

    public static final String CONSENT_ACTION_ENABLED = "Enabled";
    public static final String CONSENT_ACTION_DISABLED = "Disabled";

    public static final String SETTING_DESCRIPTION_I18N_KEY = "telemetry.setting.description";

    public static final String SETTING_PRIVACY_POLICY_URL = "https://www.zstack.io/privacy";
    public static final String MOCK_CLOUD_BASE_URL = "https://ingest.zsvirt.io";
    public static final int MOCK_CLOUD_BASE_PORT = 11443;
    public static final long CLOUD_HTTP_TIMEOUT_MILLIS = 30_000L;

    /**
     * Classpath resource (under {@code conf/telemetry/}) pinned as the Telemetry Cloud TLS CA.
     * Same packaging layout as {@code mevoco/zcfLicenseServer/ca.pem}.
     */
    public static final String CLOUD_TLS_CA_RESOURCE = "telemetry/ca.pem";

    /**
     * DNS name in the shipped Telemetry Cloud certificate (CN / SAN). Used with
     * {@link org.zstack.utils.HTTPS.Builder#verifyHostname(String)} so MN may connect by IP
     * or alternate host without disabling hostname verification.
     */
    public static final String CLOUD_TLS_CERT_HOSTNAME = "telemetry.local";

    public static final String LICENSE_TYPE_PLACEHOLDER = "Communicate";

    public static final String LOCAL_ROOT_DIR = "/var/lib/zstack/telemetry";
    public static final String LOCAL_DIR_PERMISSION = "rwx------";

    public static final String LOCAL_REPORTS_DIR = "reports";
    public static final String LOCAL_PENDING_DIR = "pending";

    /**
     * Initial delay (seconds) before the first upload tick after tasks start.
     * Collect starts immediately; upload waits so the first pending report is more likely ready.
     */
    public static final long UPLOAD_INITIAL_DELAY_SECONDS = 30L;

    public static final String CLOUD_HEALTH_PATH = "/health";
    public static final String CLOUD_REPORTS_PATH = "/v1/reports";
    public static final String CLOUD_CHECK_UPDATE_PATH = "/v1/updates/check";

    public static final String REQUEST_TYPE_CHECK_UPDATE = "check_update";

    public static final String OS_UNKNOWN = "Unknown";
}
