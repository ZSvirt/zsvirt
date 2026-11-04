package org.zstack.zsv.telemetry.client;

import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.HTTPS;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.zsv.telemetry.TelemetryConstant;
import org.zstack.zsv.telemetry.TelemetryErrors;
import org.zstack.zsv.telemetry.collect.CheckUpdateCloudResponse;
import org.zstack.zsv.telemetry.collect.TelemetryCheckUpdateReport;
import org.zstack.zsv.telemetry.entity.TelemetryUpdateInfoView;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Outbound Telemetry Cloud client (HTTPS).
 * <p>
 * Trust model mirrors ZCF license HTTPS: pin the product-shipped CA under
 * {@code telemetry/ca.pem} and verify the certificate DNS name
 * {@link TelemetryConstant#CLOUD_TLS_CERT_HOSTNAME} (safe when the URL host is an IP
 * or differs from the cert CN). Cloud leaf certs are CN-only ({@code CN=telemetry.local},
 * empty SAN); {@link HTTPS.Builder#verifyHostname(String)} accepts that shape.
 * <p>
 * If hostname verification fails, OkHttp's exception text still prints the URL host
 * (e.g. {@code Hostname 1.2.3.4 not verified}) even though verification used
 * {@code telemetry.local} — that message does not mean the expected name was the IP.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class TelemetryHttpClient {
    private static final CLogger logger = Utils.getLogger(TelemetryHttpClient.class);

    private static volatile X509Certificate telemetryCloudCa;

    private String baseUrlWithPort = resolveBaseUrlWithPort();

    public TelemetryHttpClient withBaseUrlAndPort(String baseUrl) {
        String normalized = trimTrailingSlash(baseUrl);
        requireHttpsUrl(normalized);
        this.baseUrlWithPort = normalized;
        return this;
    }

    public ErrorableValue<String> healthCheck() {
        return httpGet(baseUrlWithPort + TelemetryConstant.CLOUD_HEALTH_PATH);
    }

    public ErrorableValue<String> uploadReport(String reportJson) {
        String gzipBase64;
        try {
            gzipBase64 = gzipToBase64(reportJson);
        } catch (IOException e) {
            throw new RuntimeException(String.format("failed to gzip telemetry report: %s", e.getMessage()), e);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Content-Encoding", "gzip");
        headers.put("X-Content-Transfer", "base64");
        return httpPost(baseUrlWithPort + TelemetryConstant.CLOUD_REPORTS_PATH, gzipBase64, headers);
    }

    /**
     * POST /v1/updates/check with plain JSON body (TelemetryCheckUpdateReport).
     */
    public ErrorableValue<TelemetryUpdateInfoView> checkUpdate(TelemetryCheckUpdateReport report) {
        String bodyJson = JSONObjectUtil.toJsonString(report);
        Map<String, String> headers = Collections.singletonMap("Content-Type", "application/json");
        ErrorableValue<String> raw = httpPost(baseUrlWithPort + TelemetryConstant.CLOUD_CHECK_UPDATE_PATH, bodyJson, headers);
        if (!raw.isSuccess()) {
            return ErrorableValue.ofErrorCode(raw.error);
        }
        try {
            return ErrorableValue.of(parseCheckUpdateResponse(raw.result));
        } catch (OperationFailureException e) {
            return ErrorableValue.ofErrorCode(e.getErrorCode());
        } catch (RuntimeException e) {
            return ErrorableValue.ofErrorCode(Platform.err(
                    TelemetryErrors.TELEMETRY_CHECK_UPDATE_RESPONSE_INVALID,
                    "invalid check-update response: %s", e.getMessage()));
        }
    }

    static TelemetryUpdateInfoView parseCheckUpdateResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw Platform.err(TelemetryErrors.TELEMETRY_CHECK_UPDATE_RESPONSE_INVALID,
                    "empty check-update response").toException();
        }
        CheckUpdateCloudResponse cloud;
        try {
            cloud = JSONObjectUtil.toObject(responseBody, CheckUpdateCloudResponse.class);
        } catch (Exception e) {
            throw Platform.err(TelemetryErrors.TELEMETRY_CHECK_UPDATE_RESPONSE_INVALID,
                    "failed to parse check-update response: %s", e.getMessage()).toException();
        }
        if (cloud == null) {
            throw Platform.err(TelemetryErrors.TELEMETRY_CHECK_UPDATE_RESPONSE_INVALID,
                    "check-update response is null").toException();
        }
        if (cloud.version == null || cloud.version.trim().isEmpty()) {
            throw Platform.err(TelemetryErrors.TELEMETRY_CHECK_UPDATE_VERSION_INVALID,
                    "missing or empty version in check-update response").toException();
        }

        TelemetryUpdateInfoView view = new TelemetryUpdateInfoView();
        view.setVersion(cloud.version.trim());
        view.setReleaseNotesZh(cloud.releaseNotesZh == null ? "" : cloud.releaseNotesZh);
        view.setReleaseNotesEn(cloud.releaseNotesEn == null ? "" : cloud.releaseNotesEn);
        return view;
    }

    /**
     * Overridable for unit/integration simulators.
     */
    protected ErrorableValue<String> httpGet(String url) {
        return callHttps("GET", url, null, Collections.emptyMap());
    }

    /**
     * Overridable for unit/integration simulators.
     */
    protected ErrorableValue<String> httpPost(String url, String body, Map<String, String> headers) {
        return callHttps("POST", url, body, headers == null ? Collections.emptyMap() : headers);
    }

    private ErrorableValue<String> callHttps(String method, String url, String body, Map<String, String> headers) {
        int timeoutSec = Math.max(1, (int) TimeUnit.MILLISECONDS.toSeconds(TelemetryConstant.CLOUD_HTTP_TIMEOUT_MILLIS));
        try {
            requireHttpsUrl(url);

            HTTPS.Builder builder;
            if ("GET".equals(method)) {
                builder = HTTPS.get().url(url);
            } else if ("POST".equals(method)) {
                builder = HTTPS.post().url(url).body(body == null ? "" : body);
            } else {
                throw new IllegalArgumentException("unsupported method: " + method);
            }
            builder.readTimeout(timeoutSec).connectTimeout(timeoutSec).writeTimeout(timeoutSec);
            for (Map.Entry<String, String> e : headers.entrySet()) {
                builder.header(e.getKey(), e.getValue());
            }
            applyHttpsTrust(builder);

            try (Response rsp = builder.callWithException()) {
                String rspBody = rsp.body() != null ? rsp.body().string() : "";
                if (!rsp.isSuccessful()) {
                    return ErrorableValue.ofErrorCode(Platform.err(
                            TelemetryErrors.TELEMETRY_UPLOAD_FAILED,
                            "telemetry cloud %s %s failed: http %s, body=%s",
                            method, url, rsp.code(), rspBody));
                }
                return ErrorableValue.of(rspBody);
            }
        } catch (OperationFailureException e) {
            return ErrorableValue.ofErrorCode(e.getErrorCode());
        } catch (Exception e) {
            logger.warn(String.format("telemetry cloud %s %s failed: %s", method, url, e.getMessage()), e);
            return ErrorableValue.ofErrorCode(Platform.err(
                    TelemetryErrors.TELEMETRY_CLOUD_UNREACHABLE,
                    "telemetry cloud %s %s failed: %s", method, url, e.getMessage())
                    .withException(e));
        }
    }

    /**
     * Reject any non-HTTPS URL (defense in depth beyond GlobalConfig regex).
     */
    static void requireHttpsUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw Platform.err(TelemetryErrors.GENERAL_ERROR, "telemetry cloud URL is empty").toException();
        }
        final URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw Platform.err(TelemetryErrors.GENERAL_ERROR,
                    "invalid telemetry cloud URL: %s", url)
                    .withException(e)
                    .toException();
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw Platform.err(TelemetryErrors.GENERAL_ERROR,
                    "telemetry cloud URL must use https://, got: %s", url).toException();
        }
    }

    private static HTTPS.Builder applyHttpsTrust(HTTPS.Builder builder) {
        return builder.pinnedCa(telemetryCloudCa()).verifyHostname(TelemetryConstant.CLOUD_TLS_CERT_HOSTNAME);
    }

    private static X509Certificate telemetryCloudCa() {
        if (telemetryCloudCa == null) {
            synchronized (TelemetryHttpClient.class) {
                if (telemetryCloudCa == null) {
                    try {
                        File caFile = PathUtil.findFileOnClassPath(TelemetryConstant.CLOUD_TLS_CA_RESOURCE, true);
                        try (InputStream in = Files.newInputStream(caFile.toPath())) {
                            telemetryCloudCa = (X509Certificate) CertificateFactory.getInstance("X.509")
                                    .generateCertificate(in);
                        }
                    } catch (Exception e) {
                        throw Platform.err(
                                TelemetryErrors.TELEMETRY_CERTIFICATE_IMPORT_FAILED,
                                "failed to import certificate: %s", e.getMessage())
                                .withException(e)
                                .toException();
                    }
                }
            }
        }
        return telemetryCloudCa;
    }

    private static String resolveBaseUrlWithPort() {
        String configured = TelemetryGlobalConfig.CLOUD_BASE_URL.value();
        if (configured == null || TelemetryConstant.CONSENT_NOT_GRANTED.equals(configured) || configured.trim().isEmpty()) {
            return TelemetryConstant.MOCK_CLOUD_BASE_URL + ":" + TelemetryConstant.MOCK_CLOUD_BASE_PORT;
        }
        String url = trimTrailingSlash(configured.trim());
        requireHttpsUrl(url);
        return url + ":" + TelemetryConstant.MOCK_CLOUD_BASE_PORT;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return TelemetryConstant.MOCK_CLOUD_BASE_URL;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String gzipToBase64(String content) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
}
