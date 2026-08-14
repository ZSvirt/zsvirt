package org.zstack.externalservice.vops;

import org.springframework.http.HttpMethod;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.rest.RestHttp;
import org.zstack.utils.gson.JSONObjectUtil;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import static org.zstack.core.Platform.err;
import static org.zstack.externalservice.vops.VOpsErrors.*;

public class VOpsClientRestHttp {
    public final VOpsClient client;
    private String path;
    private long timeoutInMillis = 10000L;
    protected String body;
    protected Map<String, String> headers = new HashMap<>();

    public VOpsClientRestHttp(VOpsClient client) {
        this.client = client;
    }

    public VOpsClientRestHttp withPath(String path) {
        this.path = path;
        return this;
    }

    public VOpsClientRestHttp withTimeoutInMillis(long timeoutInMillis) {
        this.timeoutInMillis = timeoutInMillis;
        return this;
    }

    public VOpsClientRestHttp withBody(String body) {
        this.body = body;
        return this;
    }

    public VOpsClientRestHttp withBodyJson(Object body) {
        this.body = JSONObjectUtil.toJsonString(body);
        return this;
    }

    public VOpsClientRestHttp withHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public VOpsClientRestHttp withSession(String sessionUuid) {
        return withHeader("Authorization", "OAuth " + sessionUuid);
    }

    public ErrorableValue<JsonObject> getWithErrorCode() {
        return callWithErrorCode(HttpMethod.GET);
    }

    public ErrorableValue<JsonObject> postWithErrorCode() {
        return callWithErrorCode(HttpMethod.POST);
    }

    public ErrorableValue<JsonObject> putWithErrorCode() {
        return callWithErrorCode(HttpMethod.PUT);
    }

    public ErrorableValue<JsonObject> callWithErrorCode(HttpMethod method) {
        long expectEndTime = this.client.getCurrentTimeMillis() + this.timeoutInMillis;
        final RestHttp<JsonObject> http = client.http(JsonObject.class)
                .withPath(path)
                .withTimeoutInMillis(timeoutInMillis)
                .withoutRetry();
        if (this.headers != null) {
            for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                http.withHeader(entry.getKey(), entry.getValue());
            }
        }
        if (this.body != null) {
            http.withBody(this.body);
        }
        ErrorableValue<JsonObject> firstResult = http.callWithErrorCode(method);
        if (!firstResult.isSuccess()) {
            return firstResult;
        }

        if (!firstResult.result.has("api_id")) {
            return firstResult;
        }
        String apiId = firstResult.result.get("api_id").getAsString();

        RestHttp<JsonObject> nextHttp = client.http(JsonObject.class)
                .withPath(String.format("http://%s:%s/api/%s", client.hostname, client.port, apiId))
                .withTimeoutInMillis(2000L)
                .withoutRetry();
        if (this.headers != null) {
            for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                http.withHeader(entry.getKey(), entry.getValue());
            }
        }
        int waitingCount = 0;
        while (this.client.getCurrentTimeMillis() < expectEndTime) {
            ErrorableValue<JsonObject> nextResult = nextHttp.getWithErrorCode();
            if (!nextResult.isSuccess()) {
                return nextResult;
            }

            if (!nextResult.result.has("finished")) {
                return nextResult;
            }

            ++waitingCount;
            if (nextResult.result.get("finished").getAsBoolean()) {
                if (!nextResult.result.has("success") || !nextResult.result.get("success").getAsBoolean()) {
                    return ErrorableValue.ofErrorCode(err(REMOTE_AGENT_ERROR, "error on remote node")
                            .withCause(wrapErrorFromVOpsClient(nextResult.result.getAsJsonObject("error"))
                                    .withOpaque("response.by", "vops")
                                    .withOpaque("path", path)
                                    .withOpaque("api.id", apiId)));
                }

                return ErrorableValue.of(nextResult.result.getAsJsonObject("results"));
            }

            try {
                Thread.sleep(waitingCount < 10 ? 500 : 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return ErrorableValue.ofErrorCode(err(HTTP_TIMED_OUT, "client http timeout")
                .withOpaque("response.by", "vops")
                .withOpaque("path", path)
                .withOpaque("api.id", apiId));
    }
}
