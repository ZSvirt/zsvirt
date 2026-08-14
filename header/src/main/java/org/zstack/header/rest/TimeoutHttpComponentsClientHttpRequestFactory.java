package org.zstack.header.rest;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;

/**
 * Created by lining on 2017/6/12.
 */
public class TimeoutHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {
    private static final CLogger logger = Utils.getLogger(TimeoutHttpComponentsClientHttpRequestFactory.class);

    private static final ThreadLocal<TimeoutConfig> timeoutConfig = new ThreadLocal();

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {

        ClientHttpRequest request = super.createRequest(uri, httpMethod);

        TimeoutConfig config = timeoutConfig.get();
        if(config == null){
            return request;
        }
        timeoutConfig.remove();

        try {
            Field httpContextField = request.getClass().getDeclaredField("httpContext");
            httpContextField.setAccessible(true);
            HttpContext httpContext = (HttpContext) httpContextField.get(request);
            RequestConfig requestConfig = (RequestConfig) httpContext.getAttribute("http.request-config");

            Field connectTimeoutField = requestConfig.getClass().getDeclaredField("connectTimeout");
            connectTimeoutField.setAccessible(true);
            connectTimeoutField.set(requestConfig, config.connectTimeout);
            Field socketTimeoutField = requestConfig.getClass().getDeclaredField("socketTimeout");
            socketTimeoutField.setAccessible(true);
            socketTimeoutField.set(requestConfig, config.readTimeout);

        }catch (Throwable t){
            throw new IOException(t.getCause());
        }

        return request;
    }

    public void setRequestTimeoutConfig(long connectTimeout, long readTimeout){
        if(connectTimeout > Integer.MAX_VALUE){
            logger.warn(String.format("MyRestTemplate connectTimeout[%s] bigger than Integer.MAX_VALUE", connectTimeout));
        }
        if(readTimeout > Integer.MAX_VALUE){
            logger.warn(String.format("MyRestTemplate readTimeout[%s] bigger than Integer.MAX_VALUE", readTimeout));
        }

        timeoutConfig.set(new TimeoutConfig(connectTimeout, readTimeout));
    }

    static class TimeoutConfig {
        public int connectTimeout;
        public int readTimeout;

        public TimeoutConfig(long connectTimeout, long readTimeout) {
            if(connectTimeout > Integer.MAX_VALUE){
                this.connectTimeout = Integer.MAX_VALUE;
            }else{
                this.connectTimeout = (int) connectTimeout;
            }

            if(readTimeout > Integer.MAX_VALUE){
                this.readTimeout = Integer.MAX_VALUE;
            }else {
                this.readTimeout = (int) readTimeout;
            }
        }
    }
}



