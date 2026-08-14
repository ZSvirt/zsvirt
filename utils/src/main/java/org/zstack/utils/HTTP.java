package org.zstack.utils;

import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HTTP {
    private static final CLogger logger = Utils.getLogger(HTTP.class);

    private static OkHttpClient http;

    public static class HTTPFailureException extends RuntimeException {
        public int code;
        public String body;

        public HTTPFailureException(int code, String body) {
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
        ob.addInterceptor(hlogger).retryOnConnectionFailure(true);
        http = ob.build();
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
    }

    public static class Builder {
        private Param param = new Param();
        private OkHttpClient client = http;
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

            List<String> lst = param.queryParameters.computeIfAbsent(k, x->new ArrayList<>());
            lst.add(v);
            return this;
        }

        public Builder header(String k, String v) {
            if (param.headers == null) {
                param.headers = new HashMap<>();
            }

            List<String> lst = param.headers.computeIfAbsent(k ,x-> new ArrayList<>());
            lst.add(v);
            return this;
        }

        public Builder body(String v) {
            param.body = v;
            return this;
        }

        public Builder body(Object v) {
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
                    return; // skip null entries
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
                    throw new HTTPFailureException(rsp.code(), body);
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

            if (param.logging || param.readTimeout != null || param.connectTimeout != null || param.writeTimeout != null) {
                OkHttpClient.Builder ob = http.newBuilder();
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
