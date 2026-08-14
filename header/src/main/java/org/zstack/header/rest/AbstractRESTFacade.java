package org.zstack.header.rest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRESTFacade implements RESTFacade {
    public <T> T syncJsonPost(String url, Object body, Class<T> returnClass) {
        return http(returnClass)
                .withPath(url)
                .withBodyJson(body)
                .post();
    }

    public <T> T syncJsonPost(String url, Object body, Class<T> returnClass, TimeUnit unit, long timeout) {
        if (unit == null) {
            return AbstractRESTFacade.this.syncJsonPost(url, body, returnClass);
        }
        return http(returnClass)
                .withPath(url)
                .withBodyJson(body)
                .withTimeoutInMillis(unit.toMillis(timeout))
                .post();
    }

    public <T> T syncJsonPost(String url, String body, Class<T> returnClass) {
        return http(returnClass)
                .withPath(url)
                .withBody(body)
                .post();
    }

    public <T> T syncJsonPost(String url, String body, Map<String, String> headers, Class<T> returnClass) {
        final RestHttp<T> http = http(returnClass)
                .withPath(url)
                .withBody(body);
        if (headers != null) {
            headers.forEach(http::withHeader);
        }
        return http.post();
    }

    public <T> T syncJsonPost(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout) {
        if (unit == null) {
            return AbstractRESTFacade.this.syncJsonPost(url, body, headers, returnClass);
        }
        final RestHttp<T> http = http(returnClass)
                .withPath(url)
                .withBody(body)
                .withTimeoutInMillis(unit.toMillis(timeout));
        if (headers != null) {
            headers.forEach(http::withHeader);
        }
        return http.post();
    }

    /**
     * ZStack's agents only use sync/async post method
     * delete and get methods used for outsides plugins
     */
    public <T> T syncJsonDelete(String url, String body, Map<String, String> headers, Class<T> returnClass) {
        final RestHttp<T> http = http(returnClass)
                .withPath(url)
                .withBody(body);
        if (headers != null) {
            headers.forEach(http::withHeader);
        }
        return http.delete();
    }

    public <T> T syncJsonDelete(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout) {
        if (unit == null) {
            return AbstractRESTFacade.this.syncJsonDelete(url, body, headers, returnClass);
        }
        final RestHttp<T> http = http(returnClass)
                .withPath(url)
                .withBody(body)
                .withTimeoutInMillis(unit.toMillis(timeout));
        if (headers != null) {
            headers.forEach(http::withHeader);
        }
        return http.delete();
    }

    public <T> T syncJsonGet(String url, String body, Map<String, String> headers, Class<T> returnClass) {
        final RestHttp<T> http = http(returnClass)
                .withPath(url)
                .withBody(body);
        if (headers != null) {
            headers.forEach(http::withHeader);
        }
        return http.get();
    }

    public <T> T syncJsonGet(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout) {
        if (unit == null) {
            return AbstractRESTFacade.this.syncJsonGet(url, body, headers, returnClass);
        }
        final RestHttp<T> http = http(returnClass)
                .withPath(url)
                .withBody(body)
                .withTimeoutInMillis(unit.toMillis(timeout));
        if (headers != null) {
            headers.forEach(http::withHeader);
        }
        return http.get();
    }

    public <T> T syncJsonPut(String url, String body, Map<String, String> headers, Class<T> returnClass) {
        final RestHttp<T> http = http(returnClass)
                .withPath(url)
                .withBody(body);
        if (headers != null) {
            headers.forEach(http::withHeader);
        }
        return http.put();
    }

    public <T> T syncJsonPut(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout) {
        if (unit == null) {
            return AbstractRESTFacade.this.syncJsonPut(url, body, headers, returnClass);
        }
        final RestHttp<T> http = http(returnClass)
                .withPath(url)
                .withBody(body)
                .withTimeoutInMillis(unit.toMillis(timeout));
        if (headers != null) {
            headers.forEach(http::withHeader);
        }
        return http.put();
    }
}
