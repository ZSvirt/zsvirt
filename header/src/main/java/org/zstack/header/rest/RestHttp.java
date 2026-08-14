package org.zstack.header.rest;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.zstack.header.Confirm;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.zstack.header.Confirm.*;

public class RestHttp<T> {
    public final Class<T> returnClass;

    // basic info
    protected String path;
    protected Map<String, String> headers = new HashMap<>();
    protected String body;
    protected HttpMethod method;

    // request parameters
    protected boolean timeoutEnabled = true;
    protected long timeoutInMillis = 1800000L; // -1 means never timeout
    protected boolean retry = true;
    protected int retryTimes = 5;
    protected int retryIntervalInSeconds = 1;

    // response handlers
    protected BiFunction<Exception, RestHttp<T>, ErrorCode> errorCodeBuilder;
    protected Function<RestHttp<T>, ResponseEntity<String>> handler;
    protected final List<Function<Throwable, Confirm>> retryIfExceptionMatched = new ArrayList<>();

    public RestHttp(Class<T> returnClass) {
        this.returnClass = Objects.requireNonNull(returnClass);
    }

    public RestHttp<T> withPath(String path) {
        this.path = path;
        return this;
    }

    public RestHttp<T> withHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public RestHttp<T> withBody(String body) {
        this.body = body;
        return this;
    }

    public RestHttp<T> withBodyJson(Object body) {
        this.body = JSONObjectUtil.toJsonString(body);
        return this;
    }

    public RestHttp<T> withTimeoutInMillis(long timeoutInMillis) {
        this.timeoutEnabled = true;
        this.timeoutInMillis = timeoutInMillis;
        return this;
    }

    /**
     * Not recommended
     */
    public RestHttp<T> withoutTimeout() {
        this.timeoutEnabled = false;
        this.timeoutInMillis = -1L;
        return this;
    }

    public RestHttp<T> withRetry(int retryTimes, int retryIntervalInSeconds) {
        this.retry = true;
        this.retryTimes = retryTimes;
        this.retryIntervalInSeconds = retryIntervalInSeconds;
        return this;
    }

    public RestHttp<T> withoutRetry() {
        this.retry = false;
        return this;
    }

    public RestHttp<T> retryIfException(Function<Throwable, Confirm> checker) {
        this.retryIfExceptionMatched.add(checker);
        return this;
    }

    public RestHttp<T> retryIfException(Class<?>... classes) {
        return retryIfException(e -> TypeUtils.isTypeOf(e, classes) ? Yes : No);
    }

    public RestHttp<T> withHandler(Function<RestHttp<T>, ResponseEntity<String>> handler) {
        this.handler = handler;
        return this;
    }

    public RestHttp<T> withErrorCodeBuilder(BiFunction<Exception, RestHttp<T>, ErrorCode> errorCodeBuilder) {
        this.errorCodeBuilder = errorCodeBuilder;
        return this;
    }

    @SuppressWarnings("unchecked")
    protected ErrorableValue<T> handleWithErrorCode() {
        try {
            DebugUtils.Assert(this.handler != null, "handler cannot be null");
            final ResponseEntity<String> entity = this.handler.apply(this);
            if (returnClass == Void.class) {
                return ErrorableValue.of(null);
            }

            return returnClass == String.class ?
                    ((ErrorableValue<T>) ErrorableValue.of(entity.getBody())) :
                    ErrorableValue.of(JSONObjectUtil.toObject(entity.getBody(), returnClass));
        } catch (OperationFailureException e) {
            return ErrorableValue.ofErrorCode(e.getErrorCode());
        }
    }

    public T get() {
        return call(HttpMethod.GET);
    }

    public ErrorableValue<T> getWithErrorCode() {
        return callWithErrorCode(HttpMethod.GET);
    }

    public T post() {
        return call(HttpMethod.POST);
    }

    public ErrorableValue<T> postWithErrorCode() {
        return callWithErrorCode(HttpMethod.POST);
    }

    public T put() {
        return call(HttpMethod.PUT);
    }

    public ErrorableValue<T> putWithErrorCode() {
        return callWithErrorCode(HttpMethod.PUT);
    }

    public T delete() {
        return call(HttpMethod.DELETE);
    }

    public ErrorableValue<T> deleteWithErrorCode() {
        return callWithErrorCode(HttpMethod.DELETE);
    }

    public T call(HttpMethod method) {
        this.method = method;
        DebugUtils.Assert(this.handler != null, "handler cannot be null");
        final ResponseEntity<String> entity = this.handler.apply(this);
        return returnClass == Void.class ? null : JSONObjectUtil.toObject(entity.getBody(), returnClass);
    }

    public ErrorableValue<T> callWithErrorCode(HttpMethod method) {
        this.method = method;
        return handleWithErrorCode();
    }

    public ResponseEntity<String> exchange(HttpMethod method) {
        this.method = method;
        return this.handler.apply(this);
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public boolean isTimeoutEnabled() {
        return timeoutEnabled;
    }

    public long getTimeoutInMillis() {
        return timeoutInMillis;
    }

    public boolean isRetry() {
        return retry;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public int getRetryIntervalInSeconds() {
        return retryIntervalInSeconds;
    }

    public BiFunction<Exception, RestHttp<T>, ErrorCode> getErrorCodeBuilder() {
        return errorCodeBuilder;
    }

    public List<Function<Throwable, Confirm>> getRetryIfExceptionMatched() {
        return retryIfExceptionMatched;
    }
}
