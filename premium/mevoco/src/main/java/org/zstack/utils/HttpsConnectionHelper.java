package org.zstack.utils;

import org.apache.commons.codec.binary.Base64;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.RestHttp;
import org.zstack.utils.logging.CLogger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import static org.zstack.core.Platform.operr;

public class HttpsConnectionHelper  {
    private static final CLogger logger = Utils.getLogger(HttpsConnectionHelper.class);

    public final static HostnameVerifier DO_NOT_VERIFY = (hostname, session) -> true;

    public static void trustAllHosts() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                            throws java.security.cert.CertificateException {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                            throws java.security.cert.CertificateException {
                    }
                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception ex) {
            logger.warn("[vc] failed to set default SSL socket factory", ex);
        }
    }

    public static ErrorCode calculateAccessKeySignature(RestHttp<?> http,
            String accessKeyId,
            String accessKeySecret,
            String httpMethod) {
        ZonedDateTime date = ZonedDateTime.now();
        String dateStr = date.format(new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("EEE, dd MMM yyyy HH:mm:ss VV")
                .toFormatter(Locale.ENGLISH));

        final String fullPath = http.getPath();
        int slashIndex = fullPath.indexOf("/", 8);
        int queryIndex = fullPath.indexOf("?");
        String path = queryIndex == -1 ? fullPath.substring(slashIndex) : fullPath.substring(slashIndex, queryIndex);
        if (path.startsWith("/zstack/v1/")) {
            path = path.substring("/zstack".length());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(httpMethod).append("\n");
        sb.append(dateStr).append("\n").append(path);

        String sign;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret = new SecretKeySpec(accessKeySecret.getBytes(), "HmacSHA1");
            mac.init(secret);
            sign = new String(Base64.encodeBase64(mac.doFinal(sb.toString().getBytes())));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return operr("failed to generate access secret")
                    .withOpaque("exception", e.getMessage());
        }

        http.withHeader("date", dateStr);
        http.withHeader("Authorization", String.format("ZStack %s:%s", accessKeyId, sign));
        return null;
    }
}
