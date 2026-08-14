package org.zstack.expon.sdk;

import com.google.common.base.CaseFormat;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.http.HttpMethod;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.expon.sdk.volume.GetVolumeTaskProgressRequest;
import org.zstack.expon.sdk.volume.GetVolumeTaskProgressResponse;
import org.zstack.header.expon.Constants;
import org.zstack.header.rest.DefaultSSLVerifier;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExponClient {
    private static final CLogger logger = Utils.getLogger(ExponClient.class);
    private static OkHttpClient http = new OkHttpClient();

    static final Gson gson;
    private static final DateTimeFormatter formatter;

    private static final long ACTION_DEFAULT_TIMEOUT = -1;
    private static final long ACTION_DEFAULT_POLLINGINTERVAL = -1;

    static {
        gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("EEE, dd MMM yyyy HH:mm:ss VV")
                .toFormatter(Locale.ENGLISH);
    }

    private ExponConnectConfig config;

    private Supplier<String> sessionRefresher;

    private Supplier<String> sessionGetter;

    public ExponConnectConfig getConfig() {
        return config;
    }

    public void configure(ExponConnectConfig c) {
        config = c;

        if (c.readTimeout != null || c.writeTimeout != null) {
            OkHttpClient.Builder b = new OkHttpClient.Builder();

            if (c.readTimeout != null) {
                b.readTimeout(c.readTimeout, TimeUnit.MILLISECONDS);
            }
            if (c.writeTimeout != null) {
                b.writeTimeout(c.writeTimeout, TimeUnit.MILLISECONDS);
            }

            SSLSocketFactory factory = DefaultSSLVerifier.getSSLFactory(DefaultSSLVerifier.trustAllCerts);
            if (factory != null) {
                http = b.sslSocketFactory(factory, (X509TrustManager) DefaultSSLVerifier.trustAllCerts[0])
                        .hostnameVerifier(DefaultSSLVerifier::verify)
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .build();
            } else {
                http = b.followRedirects(true)
                        .followSslRedirects(true)
                        .build();
            }
        }
    }

    public void setSessionRefresher(Supplier<String> sessionRefresher) {
        this.sessionRefresher = sessionRefresher;
    }

    public void setSessionGetter(Supplier<String> sessionGetter) {
        this.sessionGetter = sessionGetter;
    }

    public <T extends ExponResponse> T call(ExponRequest req, Class<T> clz) {
        errorIfNotConfigured();
        ApiResult ret = new Api(req).call();

        if (ret.error != null) {
            ExponResponse rsp = new ExponResponse();
            rsp.retCode = ret.error.retCode;
            rsp.message = ret.error.message;
            return JSONObjectUtil.rehashObject(rsp, clz);
        }

        return ret.getResult(clz);
    }

    public void call(ExponRequest req, InternalCompletion completion) {
        errorIfNotConfigured();
        new Api(req).call(completion);
    }

    class Api {
        ExponRequest action;
        ExponRestRequest restInfo;

        InternalCompletion completion;

        final String taskIdForLog = Platform.getUuid();
        String reqBody; // for log

        Api(ExponRequest action) {
            this.action = action;
            this.restInfo = action.getClass().getAnnotation(ExponRestRequest.class);
        }

        private String substituteUrl(String url, Map<String, Object> tokens) {
            Pattern pattern = Pattern.compile("\\{(.+?)\\}");
            Matcher matcher = pattern.matcher(url);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String varName = matcher.group(1);
                Object replacement = tokens.get(varName);
                if (replacement == null) {
                    throw new ExponApiException(String.format("cannot find value for URL variable[%s]", varName));
                }

                matcher.appendReplacement(buffer, "");
                buffer.append(replacement.toString());
            }

            matcher.appendTail(buffer);
            return buffer.toString();
        }

        private List<String> getVarNamesFromUrl(String url) {
            Pattern pattern = Pattern.compile("\\{(.+?)\\}");
            Matcher matcher = pattern.matcher(url);

            List<String> urlVars = new ArrayList<>();
            while (matcher.find()) {
                urlVars.add(matcher.group(1));
            }

            return urlVars;
        }


        ApiResult doCall() {
            if (action.sessionId == null && action.getParameterMap().containsKey("sessionId")) {
                logger.info("session id is null, refresh it");
                action.sessionId = sessionRefresher.get();
                logger.info(String.format("refresh session id[%s] success", action.sessionId));
            }

            Request.Builder reqBuilder = new Request.Builder();
            action.checkParameters();

            try {
                if (action instanceof ExponQueryRequest) {
                    fillQueryApiRequestBuilder(reqBuilder);
                } else {
                    fillNonQueryApiRequestBuilder(reqBuilder);
                }
            } catch (Exception e) {
                throw new ExponApiException(e);
            }

            Request request = reqBuilder.build();

            logger.debug(String.format("call request[%s: %s]: %s, body: %s", action.getClass().getSimpleName(), taskIdForLog,
                    request.toString(), reqBody));

            try {
                try (Response response = http.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        if (response.body() == null) {
                            return httpError(response.code(), null);
                        } else {
                            return httpError(response.code(), response.body().string());
                        }
                    }

                    if (response.code() != 200 && response.code() != 204) {
                        throw new ExponApiException(String.format("[Internal Error] the server returns an unknown status code[%s]", response.code()));
                    }

                    ApiResult res = writeApiResult(response);
                    if (res.error != null) {
                        return res;
                    }

                    boolean async = !restInfo.sync() && restInfo.method() != HttpMethod.GET;
                    Map rsp = res.getResult(LinkedHashMap.class);
                    Object taskId = rsp.getOrDefault("task_id", null);
                    if (async && taskId != null) {
                        logger.debug(String.format("request[%s: %s] async result: %s", action.getClass().getSimpleName(),
                                taskIdForLog, res.getResultString()));
                        return pollResult(taskId.toString());
                    } else {
                        return res;
                    }
                }
            } catch (IOException e) {
                throw new ExponApiException(e);
            }
        }

        private ApiResult pollResult(String taskId) {
            if (this.completion == null) {
                return syncPollResult(taskId);
            } else {
                asyncPollResult(taskId);
                return null;
            }
        }

        private void fillQueryApiRequestBuilder(Request.Builder reqBuilder) throws Exception {
            ExponQueryRequest qaction = (ExponQueryRequest) action;

            HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("https")
                    .host(config.hostname)
                    .port(config.port);

            urlBuilder.addPathSegment("api");
            urlBuilder.addPathSegment(restInfo.version());

            List<String> varNames = getVarNamesFromUrl(restInfo.path());
            String path = restInfo.path();
            if (!varNames.isEmpty()) {
                Map<String, Object> vars = new HashMap<>();
                for (String vname : varNames) {
                    Object value = action.getParameterValue(vname);

                    if (value == null) {
                        throw new ExponApiException(String.format("missing required field[%s]", vname));
                    }

                    vars.put(vname, value);
                }

                path = substituteUrl(path, vars);
            }
            urlBuilder.addPathSegments(path.replaceFirst("/", ""));

            if (!qaction.conditions.isEmpty()) {
                for (String cond : qaction.conditions) {
                    String[] kv = cond.split("=");
                    urlBuilder.addQueryParameter(kv[0], kv[1]);
                }
            }
            if (qaction.limit != null) {
                urlBuilder.addQueryParameter("offset", String.format("%s", qaction.limit));
            }
            if (qaction.start != null) {
                urlBuilder.addQueryParameter("index", String.format("%s", qaction.start));
            }
            if (qaction.sortBy != null) {
                urlBuilder.addQueryParameter("sort_by", String.format("%s", qaction.sortBy));
            }
            if (qaction.sortDirection != null) {
                urlBuilder.addQueryParameter("order_by", String.format("%s", qaction.sortBy));
            }

            reqBuilder.addHeader(Constants.HEADER_AUTHORIZATION, String.format("%s %s", Constants.BEARER, qaction.sessionId));

            reqBuilder.url(urlBuilder.build()).get();
        }

        private void fillNonQueryApiRequestBuilder(Request.Builder reqBuilder) throws Exception {
            HttpUrl.Builder builder = new HttpUrl.Builder()
                    .scheme("https")
                    .host(config.hostname)
                    .port(config.port);
            builder.addPathSegment("api");
            builder.addPathSegment(restInfo.version());
            if (restInfo.sync()) {
                builder.addPathSegment("sync");
            }

            List<String> varNames = getVarNamesFromUrl(restInfo.path());
            String path = restInfo.path();
            if (!varNames.isEmpty()) {
                Map<String, Object> vars = new HashMap<>();
                for (String vname : varNames) {
                    Object value = action.getParameterValue(vname);

                    if (value == null) {
                        throw new ExponApiException(String.format("missing required field[%s]", vname));
                    }

                    vars.put(vname, value);
                }

                path = substituteUrl(path, vars);
                builder.addPathSegments(path.replaceFirst("/", ""));
            } else {
                builder.addPathSegments(path.replaceFirst("/", ""));
            }

            final Map<String, Object> params = new HashMap<>();

            for (String pname : action.getAllParameterNames()) {
                if (varNames.contains(pname) || Constants.SESSION_ID.equals(pname)) {
                    // the field is set in URL variables
                    continue;
                }

                Object value = action.getParameterValue(pname);
                if (value != null) {
                    params.put(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, pname), value);
                }
            }

            if (restInfo.method().equals(HttpMethod.GET)) {
                reqBuilder.url(builder.build()).get();
            } else if (restInfo.method().equals(HttpMethod.DELETE) && !restInfo.hasBody()) {
                params.forEach((k, v) -> builder.addQueryParameter(k, v.toString()));
                reqBuilder.url(builder.build()).delete();
            } else {
                reqBody = gson.toJson(params);
                reqBuilder.url(builder.build()).method(restInfo.method().toString(), RequestBody.create(Constants.JSON, reqBody));
            }

            if (action instanceof LoginExponRequest) {
                return;
            }
            reqBuilder.addHeader(Constants.HEADER_AUTHORIZATION, String.format("%s %s", Constants.BEARER, action.sessionId));
        }

        private ErrorCode errorCode(String id, String s) {
            ErrorCode err = new ErrorCode();
            err.retCode = id;
            err.message = s;
            return err;
        }

        private ApiResult syncPollResult(String taskId) {
            long current = System.currentTimeMillis();
            long timeout = this.getTimeout();
            long expiredTime = current + timeout;
            long interval = this.getInterval();

            ApiResult ret = new ApiResult();
            ExponResponse rsp = new ExponResponse();
            while (current < expiredTime) {
                ExponTask trsp = getTaskStatus(taskId);
                if (TaskStatus.SUCCESS.name().equals(trsp.getStatus()) || TaskStatus.FAILED.name().equals(trsp.getStatus())) {
                    rsp.setRetCode(trsp.getRetCode());
                    rsp.setMessage(trsp.getRetMsg());
                    ret.setResultString(gson.toJson(rsp));
                    return ret;
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(interval);
                } catch (InterruptedException e) {
                    // ignore
                }
                current += interval;
            }

            ApiResult res = new ApiResult();
            res.error = errorCode(
                    Constants.POLLING_TIMEOUT_ERROR,
                    String.format("polling result of api[%s] timeout after %s ms", action.getClass().getSimpleName(), timeout)
            );

            return res;
        }

        private void asyncPollResult(final String taskId) {
            final long current = System.currentTimeMillis();
            final long timeout = this.getTimeout();
            final long expiredTime = current + timeout;
            final long i = this.getInterval();
            final int[] pollTimes = {0};

            final Timer timer = new Timer();

            timer.schedule(new TimerTask() {
                long count = current;
                final long interval = i;

                private void done(ApiResult res) {
                    logger.debug(String.format("request[%s: %s] result: %s", action.getClass().getSimpleName(),
                            taskIdForLog, res.getResultString()));
                    completion.complete(res);
                    timer.cancel();
                }

                @Override
                public void run() {
                    try {
                        ExponTask trsp = getTaskStatus(taskId);
                        // TODO: remove this when the expon server is ready for zero copy
                        if (CoreGlobalProperty.UNIT_TEST_ON && pollTimes[0]++ > 2) {
                            trsp.setStatus(TaskStatus.SUCCESS.name());
                        }

                        if (TaskStatus.SUCCESS.name().equals(trsp.getStatus()) || TaskStatus.FAILED.name().equals(trsp.getStatus())) {

                            ApiResult ret = new ApiResult();
                            ExponResponse rsp = new ExponResponse();

                            rsp.setRetCode(trsp.getRetCode());
                            rsp.setMessage(trsp.getRetMsg());
                            if (!trsp.isSuccess()) {
                                ret.error = errorCode(trsp.getRetCode(), trsp.getRetMsg());
                            }
                            ret.setResultString(gson.toJson(rsp));
                            done(ret);
                            return;
                        }

                        count += interval;
                        if (count >= expiredTime) {
                            ApiResult res = new ApiResult();
                            res.error = errorCode(
                                    Constants.POLLING_TIMEOUT_ERROR,
                                    String.format("polling result of api[%s] timeout after %s ms", action.getClass().getSimpleName(), timeout)
                            );

                            done(res);
                        }
                    } catch (Throwable e) {
                        ApiResult res = new ApiResult();
                        res.error = errorCode(
                                Constants.INTERNAL_ERROR,
                                "an internal error happened: " + e.getMessage()
                        );

                        done(res);
                    }
                }
            }, 0, i);
        }

        private ExponTask getTaskStatus(String taskId) {
            ExponTask task;
            if (NumberUtils.isNumber(taskId)) {
                GetVolumeTaskProgressResponse rsp = getVolumeTaskProgress(Float.valueOf(taskId).intValue());
                if (!rsp.isSuccess()) {
                    throw new ExponApiException(String.format("failed to get task status, %s", rsp.getMessage()));
                }

                task = ExponTask.valueOf(rsp.getTask());
            } else {
                GetTaskStatusResponse rsp = getTaskDetail(taskId);
                if (!rsp.isSuccess()) {
                    throw new ExponApiException(String.format("failed to get task status, %s", rsp.getMessage()));
                }

                task = ExponTask.valueOf(rsp);
            }

            return task;
        }

        private GetTaskStatusResponse getTaskDetail(String taskId) {
            GetTaskStatusRequest req = new GetTaskStatusRequest();
            req.setId(taskId);
            req.setSessionId(sessionGetter.get());
            return retrySessionGetResult(req, GetTaskStatusResponse.class);
        }

        private GetVolumeTaskProgressResponse getVolumeTaskProgress(int taskId) {
            GetVolumeTaskProgressRequest req = new GetVolumeTaskProgressRequest();
            req.setTaskId(taskId);
            req.setSessionId(sessionGetter.get());
            return retrySessionGetResult(req, GetVolumeTaskProgressResponse.class);
        }

        private <T extends ExponResponse> T retrySessionGetResult(ExponRequest req, Class<T> clz) {
            T rsp = ExponClient.this.call(req, clz);
            if (!rsp.sessionExpired()) {
                return rsp;
            }

            logger.debug(String.format("session[%s] expired, login again", req.sessionId));
            this.action.sessionId = sessionRefresher.get();
            req.setSessionId(sessionGetter.get());
            return ExponClient.this.call(req, clz);
        }

        private ApiResult writeApiResult(Response response) throws IOException {
            ApiResult res = new ApiResult();

            if (response.code() == 200) {
                String body = response.body().string();
                ExponResponse rsp = gson.fromJson(body, ExponResponse.class);
                if (rsp.isSuccess()) {
                    res.setResultString(body);
                } else {
                    res.error = errorCode(rsp.retCode, StringEscapeUtils.unescapeJava(rsp.message));
                }
            } else {
                throw new ExponApiException(String.format("unknown status code: %s", response.code()));
            }
            return res;
        }

        private ApiResult httpError(int code, String details) {
            ApiResult res = new ApiResult();
            if (details != null && details.startsWith("{")) {
                ExponResponse rsp = gson.fromJson(details, ExponResponse.class);
                if (rsp.message != null) {
                    res.error = errorCode(
                            rsp.retCode,
                            StringEscapeUtils.unescapeJava(rsp.message)
                    );
                    return res;
                }
            }

            res.error = errorCode(
                    Constants.HTTP_ERROR,
                    String.format("the http status code[%s] details[%s] indicates a failure happened", code, details)
            );
            return res;
        }

        ApiResult call() {
            ApiResult ret = doCall();
            if (ret.error != null) {
                logger.debug(String.format("request[%s: %s] error: %s result: %s", action.getClass().getSimpleName(),
                        taskIdForLog, gson.toJson(ret.error), ret.getResultString()));
            } else {
                logger.debug(String.format("request[%s: %s] result: %s", action.getClass().getSimpleName(),
                        taskIdForLog, ret.getResultString()));
            }
            return ret;
        }

        void call(InternalCompletion completion) {
            this.completion = completion;
            ApiResult res = doCall();
            if (res != null) {
                completion.complete(res);
            }
        }

        private long getTimeout(){
            return action.timeout == ACTION_DEFAULT_TIMEOUT ? config.defaultPollingTimeout: action.timeout;
        }

        private long getInterval(){
            return config.defaultPollingInterval;
        }
    }

    private void errorIfNotConfigured() {
        if (config == null) {
            throw new RuntimeException("setConfig() must be called before any methods");
        }
    }
}