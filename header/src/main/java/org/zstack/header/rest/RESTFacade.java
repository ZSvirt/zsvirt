package org.zstack.header.rest;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.zstack.header.core.Completion;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface RESTFacade {
    void asyncJsonPost(String url, Object body, Map<String, String> headers, AsyncRESTCallback callback, TimeUnit unit, long timeout);

    void asyncJsonPost(String url, Object body, AsyncRESTCallback callback, TimeUnit unit, long timeout);

    void asyncJsonPost(String url, String body, AsyncRESTCallback callback, TimeUnit unit, long timeout);

    void asyncJsonPost(String url, String body, Map<String, String> headers, AsyncRESTCallback callback, TimeUnit unit, long timeout);

    void asyncJsonPost(String url, Object body, Map<String, String> headers, AsyncRESTCallback callback);

    void asyncJsonPost(String url, Object body, AsyncRESTCallback callback);

    void asyncJsonPost(String url, String body, AsyncRESTCallback callback);
    void asyncJsonDelete(String url, String body, Map<String, String> headers, AsyncRESTCallback callback, TimeUnit unit, long timeout);
    void asyncJsonGet(String url, String body, Map<String, String> headers, AsyncRESTCallback callback, TimeUnit unit, long timeout);

    <T> T syncJsonPost(String url, Object body, Class<T> returnClass);

    <T> T syncJsonPost(String url, Object body, Class<T> returnClass, TimeUnit unit, long timeout);

    <T> T syncJsonPost(String url, String body, Class<T> returnClass);

    <T> T syncJsonPost(String url, String body, Map<String, String> headers, Class<T> returnClass);

    <T> T syncJsonPost(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout);

    /**
     * ZStack's agents only use sync/async post method
     * delete and get methods used for outsides plugins
     */
    <T> T syncJsonDelete(String url, String body, Map<String, String> headers, Class<T> returnClass);

    <T> T syncJsonDelete(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout);

    <T> T syncJsonGet(String url, String body, Map<String, String> headers, Class<T> returnClass);

    <T> T syncJsonGet(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout);

    <T> T syncJsonPut(String url, String body, Map<String, String> headers, Class<T> returnClass);

    <T> T syncJsonPut(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout);

    <T> RestHttp<T> http(Class<T> returnClass);

    default RestHttp<String> http() {
        return http(String.class);
    }

    HttpHeaders syncHead(String url);

    HttpEntity<String> httpServletRequestToHttpEntity(HttpServletRequest req);

    RestTemplate getRESTTemplate();

    void echo(String url, Completion callback);

    void echo(String url, Completion callback, long inverval, long timeout);

    Map<String, HttpCallStatistic> getStatistics();

    <T> void registerSyncHttpCallHandler(String path, Class<T> objectType, SyncHttpCallHandler<T> handler);

    /**
     * in location of API message reply body
     */
    String getReplyLocationBaseUrl();

    String getBaseUrl();

    String getSendCommandUrl();

    String getCallbackUrl();

    String getHostName();

    int getPort();

    String makeUrl(String path);

    Runnable installBeforeAsyncJsonPostInterceptor(BeforeAsyncJsonPostInterceptor interceptor);

    static void setMessageConverter(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringHttpMessageConverter.setWriteAcceptCharset(true);

        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof StringHttpMessageConverter) {
                converters.remove(i);
                converters.add(i, stringHttpMessageConverter);
                break;
            }
        }
    }

    // timeout are in milliseconds
    static TimeoutRestTemplate createRestTemplate(int readTimeout, int connectTimeout) {
        HttpComponentsClientHttpRequestFactory factory = new TimeoutHttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(readTimeout);
        factory.setConnectTimeout(connectTimeout);
        factory.setConnectionRequestTimeout(connectTimeout * 2);

        SSLContext sslContext = DefaultSSLVerifier.getSSLContext(DefaultSSLVerifier.trustAllCerts);

        if (sslContext != null) {
            factory.setHttpClient(HttpClients.custom()
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .setSSLContext(sslContext)
                    .build());
        }

        TimeoutRestTemplate template = new TimeoutRestTemplate(factory);
        setMessageConverter(template.getMessageConverters());

        return template;
    }
}
