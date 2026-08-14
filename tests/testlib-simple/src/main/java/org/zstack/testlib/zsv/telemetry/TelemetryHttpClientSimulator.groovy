package org.zstack.testlib.zsv.telemetry

import org.springframework.http.HttpMethod
import org.zstack.core.Platform
import org.zstack.header.errorcode.ErrorableValue
import org.zstack.header.rest.RestHttp
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zsv.telemetry.TelemetryConstant
import org.zstack.zsv.telemetry.TelemetryErrors
import org.zstack.zsv.telemetry.client.TelemetryHttpClient

import java.util.function.Function

class TelemetryHttpClientSimulator extends TelemetryHttpClient {
    final TelemetryVirtualEndpointSpec parent

    /**
     * key example:
     *   "/health"
     *   "GET:/health"
     *   "POST:/v1/reports"
     */
    final Map<String, Function<HttpForTest, Object>> defaultHandlers = [:]

    TelemetryHttpClientSimulator(TelemetryVirtualEndpointSpec parent) {
        this.parent = parent
    }

    private void simulator(String pathWithoutIpAndPort, Function<HttpForTest, Object> handler) {
        defaultHandlers.put(pathWithoutIpAndPort, handler)
    }

    private void simulatorGet(String pathWithoutIpAndPort, Function<HttpForTest, Object> handler) {
        defaultHandlers.put(HttpMethod.GET.toString() + ":" + pathWithoutIpAndPort, handler)
    }

    private void simulatorPost(String pathWithoutIpAndPort, Function<HttpForTest, Object> handler) {
        defaultHandlers.put(HttpMethod.POST.toString() + ":" + pathWithoutIpAndPort, handler)
    }

    @Override
    protected ErrorableValue<String> httpGet(String url) {
        return dispatch(HttpMethod.GET, url, null, [:])
    }

    @Override
    protected ErrorableValue<String> httpPost(String url, String body, Map<String, String> headers) {
        return dispatch(HttpMethod.POST, url, body, headers ?: [:])
    }

    private ErrorableValue<String> dispatch(HttpMethod method, String url, String body, Map<String, String> headers) {
        def http = new HttpForTest<String>(String.class, this)
        http.withPath(url)
        http.method = method
        if (body != null) {
            http.withBody(body)
        }
        headers.each { k, v -> http.withHeader(k, v) }
        http.withErrorCodeBuilder({ Exception e, RestHttp<?> ignored ->
            return Platform.err(TelemetryErrors.TELEMETRY_CLOUD_UNREACHABLE, e.getMessage()).withException(e)
        })
        return http.handleWithErrorCode()
    }

    static class HttpForTest<T> extends RestHttp<T> {
        final TelemetryHttpClientSimulator client

        HttpForTest(Class<T> returnClass, TelemetryHttpClientSimulator client) {
            super(returnClass)
            this.client = client
        }

        /**
         * "https://telemetry.zstack.io/health" -> "/health"
         */
        String getPathWithoutIpAndPort() {
            assert path != null: "path cannot be null"
            int slashIndex = path.indexOf("/", 8)
            if (slashIndex == -1) {
                throw new RuntimeException("invalid path: ${path}")
            }
            int queryIndex = path.indexOf("?")
            return queryIndex == -1 ? path.substring(slashIndex) : path.substring(slashIndex, queryIndex)
        }

        @Override
        ErrorableValue<T> handleWithErrorCode() {
            def result = findValueFromHandle()

            for (def handler : client.parent.postHandlers) {
                if (handler.condition.test(this)) {
                    try {
                        def next = handler.runIfMatch.apply(this, result)
                        result = next == null ? result : next
                    } catch (Exception e) {
                        result = ErrorableValue.ofErrorCode(errorCodeBuilder.apply(e, this))
                    }
                }
            }
            return result
        }

        private ErrorableValue<T> findValueFromHandle() {
            def handler = client.defaultHandlers.get(method.toString() + ":" + getPathWithoutIpAndPort())
            if (handler == null) {
                handler = client.defaultHandlers.get(getPathWithoutIpAndPort())
            }
            if (handler == null) {
                throw new RuntimeException("no handler found for path: ${getPath()}")
            }
            return ErrorableValue.of((T) handler.apply(this))
        }
    }

    {
        simulatorGet(TelemetryConstant.CLOUD_HEALTH_PATH) { HttpForTest it ->
            return "ok"
        }

        simulatorPost(TelemetryConstant.CLOUD_REPORTS_PATH) { HttpForTest it ->
            it.client.parent.uploadedBodies << (it.body as String)
            return "ok"
        }

        simulatorPost(TelemetryConstant.CLOUD_CHECK_UPDATE_PATH) { HttpForTest it ->
            it.client.parent.checkUpdateBodies << (it.body as String)
            return JSONObjectUtil.toJsonString([
                    version           : "5.1.0",
                    release_notes_zh  : "1. Bug fixes\n2. Improve telemetry check-update",
                    release_notes_en  : "1. Bug fixes\n2. Improve telemetry check-update"
            ])
        }
    }
}
