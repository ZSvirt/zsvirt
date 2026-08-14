package org.zstack.utils;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.tls.OkHostnameVerifier;
import okhttp3.logging.HttpLoggingInterceptor;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An OkHttp-based HTTPS client.
 *
 * <p>SECURE BY DEFAULT: unless explicitly told otherwise, this uses the JVM default trust chain and
 * the default hostname verification, exactly like a normal HTTPS client.
 *
 * <p>PREFERRED for self-signed internal endpoints: when the target uses a self-signed certificate
 * whose CA is shipped with the product (e.g. the ZCF/ChaosForever license server or Telemetry Cloud,
 * reached by raw IP with a DNS-only cert), pin that CA via
 * {@link Builder#pinnedCa(java.security.cert.X509Certificate)} and verify against the certificate's
 * DNS name via {@link Builder#verifyHostname(String)}. This keeps full MITM protection without
 * needing a public CA or an IP SAN. {@code verifyHostname} also accepts CN-only certificates that
 * omit SAN (OkHttp 4+ otherwise rejects them). Note OkHttp's {@code SSLPeerUnverifiedException}
 * message always prints the URL host (e.g. an IP), even when verification used the expected DNS name.
 *
 * <p>OPT-IN trust-all (last resort): certificate and/or hostname verification can be skipped
 * <b>only</b> when the caller explicitly opts in via {@link Builder#trustAllCerts()} /
 * {@link Builder#trustAllHostnames()}. This makes "skip verification" visible at every call site and
 * mirrors the platform's relaxed TLS policy in {@code org.zstack.header.rest.DefaultSSLVerifier} /
 * {@code RESTFacade.createRestTemplate()}. Prefer pinning over trust-all whenever the CA is known.
 *
 * <p>SECURITY NOTE: opting in to trust-all disables server identity verification and is therefore
 * vulnerable to MITM. Do NOT opt in for connections to the public internet or to endpoints that
 * present a proper CA-signed certificate.
 */
public class HTTPS {
    private static final CLogger logger = Utils.getLogger(HTTPS.class);

    // trust-all is opt-in per request via Builder.trustAllCerts(), see class-level javadoc
    private static final X509TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    // accept any hostname: opt-in per request via Builder.trustAllHostnames(), for raw-IP targets
    // without a matching cert SAN, see class-level javadoc
    private static final HostnameVerifier TRUST_ALL_HOSTNAMES = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    private static OkHttpClient https;

    public static class HTTPSFailureException extends RuntimeException {
        public int code;
        public String body;

        public HTTPSFailureException(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    static {
        OkHttpClient.Builder ob = new OkHttpClient.Builder();
        HttpLoggingInterceptor hlogger = new HttpLoggingInterceptor(msg -> logger.trace(String.format("========== %s", msg)));
        if (logger.isTraceEnabled()) {
            hlogger.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            hlogger.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        ob.addInterceptor(hlogger)
                .retryOnConnectionFailure(true);
        https = ob.build();
    }

    private static SSLSocketFactory getTrustAllSslSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{TRUST_ALL_CERTS}, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("failed to create trust-all SSL socket factory", e);
        }
    }

    private static X509TrustManager trustManagerForCa(X509Certificate ca) {
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("ca", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    return (X509TrustManager) tm;
                }
            }
            throw new IllegalStateException("no X509TrustManager produced for the pinned CA");
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("failed to build trust manager for the pinned CA", e);
        }
    }

    private static SSLSocketFactory sslSocketFactoryForTrustManager(X509TrustManager tm) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{tm}, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("failed to create SSL socket factory for the pinned CA", e);
        }
    }

    /**
     * Verify the peer certificate against {@code expectedHostname} (not the URL host).
     * Prefers OkHttp's verifier (SAN / standard rules); if that fails, accept a matching CN
     * for product certs that ship without SAN (e.g. {@code CN=telemetry.local}).
     */
    static boolean verifyExpectedHostname(String expectedHostname, SSLSession session) {
        if (expectedHostname == null || expectedHostname.isEmpty() || session == null) {
            return false;
        }
        if (OkHostnameVerifier.INSTANCE.verify(expectedHostname, session)) {
            return true;
        }
        try {
            Certificate[] peer = session.getPeerCertificates();
            if (peer == null || peer.length == 0 || !(peer[0] instanceof X509Certificate)) {
                return false;
            }
            return commonNameEquals((X509Certificate) peer[0], expectedHostname);
        } catch (SSLPeerUnverifiedException e) {
            return false;
        }
    }

    public static boolean commonNameEquals(X509Certificate cert, String expectedHostname) {
        if (cert == null || expectedHostname == null) {
            return false;
        }
        String dn = cert.getSubjectX500Principal().getName();
        if (dn == null || dn.isEmpty()) {
            return false;
        }
        String expected = expectedHostname.toLowerCase(Locale.US);
        for (String part : dn.split(",")) {
            String p = part.trim();
            if (p.length() >= 3 && p.regionMatches(true, 0, "CN=", 0, 3)) {
                return expected.equals(p.substring(3).trim().toLowerCase(Locale.US));
            }
        }
        return false;
    }

    public static class Param {
        String url;
        Map<String, List<String>> headers;
        String body;
        Map<String, List<String>> queryParameters;
        String method;
        Integer readTimeout;
        Integer writeTimeout;
        Integer connectTimeout;
        boolean logging;
        boolean trustAllCerts;
        boolean trustAllHostnames;
        X509Certificate pinnedCa;
        String verifyHostname;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Map<String, List<String>> getQueryParameters() {
            return queryParameters;
        }

        public void setQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters = queryParameters;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Integer getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Integer getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(Integer writeTimeout) {
            this.writeTimeout = writeTimeout;
        }

        public Integer getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public boolean isLogging() {
            return logging;
        }

        public void setLogging(boolean logging) {
            this.logging = logging;
        }

        public boolean isTrustAllCerts() {
            return trustAllCerts;
        }

        public void setTrustAllCerts(boolean trustAllCerts) {
            this.trustAllCerts = trustAllCerts;
        }

        public boolean isTrustAllHostnames() {
            return trustAllHostnames;
        }

        public void setTrustAllHostnames(boolean trustAllHostnames) {
            this.trustAllHostnames = trustAllHostnames;
        }

        public X509Certificate getPinnedCa() {
            return pinnedCa;
        }

        public void setPinnedCa(X509Certificate pinnedCa) {
            this.pinnedCa = pinnedCa;
        }

        public String getVerifyHostname() {
            return verifyHostname;
        }

        public void setVerifyHostname(String verifyHostname) {
            this.verifyHostname = verifyHostname;
        }
    }

    public static class Builder {
        private Param param = new Param();
        private OkHttpClient client = https;
        private Request request;

        private boolean build;
        private MultipartBody.Builder multipartBody;

        public Param getParam() {
            return param;
        }

        public Builder() {
        }

        public Builder(String m) {
            param.method = m;
        }

        public Builder url(String v) {
            param.url = v;
            return this;
        }

        public Builder queryParameter(String k, String v) {
            if (param.queryParameters == null) {
                param.queryParameters = new HashMap<>();
            }

            List<String> lst = param.queryParameters.computeIfAbsent(k, x -> new ArrayList<>());
            lst.add(v);
            return this;
        }

        public Builder header(String k, String v) {
            if (param.headers == null) {
                param.headers = new HashMap<>();
            }

            List<String> lst = param.headers.computeIfAbsent(k, x -> new ArrayList<>());
            lst.add(v);
            return this;
        }

        public Builder body(String v) {
            if (multipartBody != null) {
                throw new IllegalStateException("Cannot set both body and form data");
            }
            param.body = v;
            return this;
        }

        public Builder body(Object v) {
            if (multipartBody != null) {
                throw new IllegalStateException("Cannot set both body and form data");
            }
            param.body = JSONObjectUtil.toJsonString(v);
            return this;
        }

        public Builder readTimeout(int v) {
            param.readTimeout = v;
            return this;
        }

        public Builder writeTimeout(int v) {
            param.writeTimeout = v;
            return this;
        }

        public Builder connectTimeout(int v) {
            param.connectTimeout = v;
            return this;
        }

        public Builder logging() {
            param.logging = true;
            return this;
        }

        /**
         * Skip server certificate verification for this request. SECURITY: disables MITM protection,
         * use only for trusted internal self-signed targets. See {@link HTTPS} class javadoc.
         */
        public Builder trustAllCerts() {
            param.trustAllCerts = true;
            return this;
        }

        /**
         * Skip hostname verification for this request. SECURITY: disables MITM protection, use only
         * for trusted internal targets reached by raw IP. See {@link HTTPS} class javadoc.
         */
        public Builder trustAllHostnames() {
            param.trustAllHostnames = true;
            return this;
        }

        /**
         * Pin the server trust chain to a single CA certificate for this request: the server cert is
         * accepted only if it chains to {@code ca}. This is the secure alternative to
         * {@link #trustAllCerts()} for self-signed internal endpoints whose CA is shipped with the
         * product.
         */
        public Builder pinnedCa(X509Certificate ca) {
            param.pinnedCa = ca;
            return this;
        }

        /**
         * Verify the server certificate against {@code expectedHostname} instead of the host in the
         * request URL. Use this when connecting by raw IP to a server whose certificate only carries
         * a DNS name in SAN or CN (e.g. {@code telemetry.local}); it keeps proper hostname
         * verification (the secure alternative to {@link #trustAllHostnames()}) rather than
         * disabling it.
         */
        public Builder verifyHostname(String expectedHostname) {
            param.verifyHostname = expectedHostname;
            return this;
        }

        public Builder formData(Map<String, Object> formFields) {
            if (formFields == null) {
                throw new IllegalArgumentException("formFields cannot be null");
            }

            if (param.body != null) {
                throw new IllegalStateException("Cannot set both body and form data");
            }

            multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM);
            formFields.forEach((k, v) -> {
                if (k == null || v == null) {
                    return;
                }

                if (v instanceof File) {
                    File file = (File) v;
                    RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
                    multipartBody.addFormDataPart(k, file.getName(), fileBody);
                } else {
                    multipartBody.addFormDataPart(k, v.toString());
                }
            });
            return this;
        }

        public Response callWithException() throws IOException {
            build();

            return client.newCall(request).execute();
        }

        public Response call() {
            build();

            try {
                return client.newCall(request).execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> T call(Class<T> clz) {
            build();

            try (Response rsp = call()) {
                String body = rsp.body().string();
                if (!rsp.isSuccessful()) {
                    throw new HTTPSFailureException(rsp.code(), body);
                }

                return JSONObjectUtil.toObject(body, clz);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void build() {
            if (build) {
                return;
            }

            build = true;

            HttpUrl url = HttpUrl.parse(param.url);
            HttpUrl.Builder ub = url.newBuilder();
            if (param.queryParameters != null) {
                param.queryParameters.forEach((k, lst) -> lst.forEach(v -> ub.addQueryParameter(k, v)));
            }

            Request.Builder rb = new Request.Builder();
            rb.url(ub.build());

            String contentType = "application/json";
            if (param.headers != null) {
                List<String> ls = param.headers.get("Content-Type");
                if (ls != null) {
                    contentType = ls.get(0);
                }

                param.headers.forEach((k, lst) -> lst.forEach(v -> rb.addHeader(k, v)));
            }

            if ("POST".equals(param.method)) {
                if (multipartBody != null) {
                    rb.post(multipartBody.build());
                } else {
                    DebugUtils.Assert(param.body != null, "POST requires body");
                    rb.post(RequestBody.create(param.body, MediaType.parse(contentType)));
                }
            } else if ("GET".equals(param.method)) {
                rb.get();
            } else if ("DELETE".equals(param.method)) {
                rb.delete();
            } else if ("PUT".equals(param.method)) {
                if (multipartBody != null) {
                    rb.put(multipartBody.build());
                } else {
                    DebugUtils.Assert(param.body != null, "PUT requires body");
                    rb.put(RequestBody.create(param.body, MediaType.parse(contentType)));
                }
            } else if ("HEAD".equals(param.method)) {
                rb.head();
            } else {
                throw new RuntimeException(String.format("unsupported method: %s", param.method));
            }

            request = rb.build();

            if (param.logging || param.readTimeout != null || param.connectTimeout != null
                    || param.writeTimeout != null || param.trustAllCerts || param.trustAllHostnames
                    || param.pinnedCa != null || param.verifyHostname != null) {
                OkHttpClient.Builder ob = https.newBuilder();
                if (param.readTimeout != null) {
                    ob.readTimeout(param.readTimeout, TimeUnit.SECONDS);
                }
                if (param.writeTimeout != null) {
                    ob.writeTimeout(param.writeTimeout, TimeUnit.SECONDS);
                }
                if (param.connectTimeout != null) {
                    ob.connectTimeout(param.connectTimeout, TimeUnit.SECONDS);
                }
                if (param.logging) {
                    HttpLoggingInterceptor l = new HttpLoggingInterceptor();
                    l.setLevel(HttpLoggingInterceptor.Level.BODY);
                    ob.addInterceptor(l);
                }
                if (param.trustAllCerts) {
                    ob.sslSocketFactory(getTrustAllSslSocketFactory(), TRUST_ALL_CERTS);
                } else if (param.pinnedCa != null) {
                    X509TrustManager tm = trustManagerForCa(param.pinnedCa);
                    ob.sslSocketFactory(sslSocketFactoryForTrustManager(tm), tm);
                }
                if (param.trustAllHostnames) {
                    ob.hostnameVerifier(TRUST_ALL_HOSTNAMES);
                } else if (param.verifyHostname != null) {
                    String expected = param.verifyHostname;
                    ob.hostnameVerifier((hostname, session) -> verifyExpectedHostname(expected, session));
                }

                client = ob.build();
            }
        }
    }

    public static Builder get() {
        return new Builder("GET");
    }

    public static Builder post() {
        return new Builder("POST");
    }

    public static Builder put() {
        return new Builder("PUT");
    }

    public static Builder head() {
        return new Builder("HEAD");
    }

    public static Builder delete() {
        return new Builder("DELETE");
    }
}
