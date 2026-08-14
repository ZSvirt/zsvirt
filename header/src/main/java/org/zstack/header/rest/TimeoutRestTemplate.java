package org.zstack.header.rest;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by lining on 2017/6/12.
 */
public class TimeoutRestTemplate extends RestTemplate {
    private static final CLogger logger = Utils.getLogger(TimeoutRestTemplate.class);

    static {
        Utils.registerSafeLogger(TimeoutRestTemplate.class);
    }

    public TimeoutRestTemplate(ClientHttpRequestFactory requestFactory) {
        super();
        this.setRequestFactory(requestFactory);
    }

    // connectTimeout millisecond
    // readTimeout millisecond
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType, String requestId, long connectTimeout, long readTimeout) throws RestClientException {
        assert connectTimeout >= 0;
        assert readTimeout >= 0;
        assert requestId != null;

        this.setRequestConfig(connectTimeout, readTimeout);

        long startTime = System.currentTimeMillis();
        try {
            return this.exchange(url, method, requestEntity, responseType);
        } catch (Throwable t){
            long endTime = System.currentTimeMillis();
            logger.warn(String.format("TimeoutRestTemplate exchange fail, requestId=%s, connectTimeout=%s, readTimeout=%s, spendTime=%s", requestId, connectTimeout, readTimeout, endTime - startTime), t);
            throw t;
        } finally {
            long endTime = System.currentTimeMillis();
            if(endTime - startTime > (connectTimeout + 3000) || endTime - startTime > (readTimeout + 3000) ){
                logger.error(String.format("TimeoutRestTemplate timeout error, requestId=%s, connectTimeout=%s, readTimeout=%s, spendTime=%s", requestId, connectTimeout, readTimeout, endTime - startTime));
            } else {
                logger.debug(String.format("TimeoutRestTemplate exchange method:%s, url: %s", method, url));
            }
        }
    }

    private void setRequestConfig(long connectTimeout, long readTimeout){
        this.getMyRequestFactory().setRequestTimeoutConfig(connectTimeout, readTimeout);
    }


    private TimeoutHttpComponentsClientHttpRequestFactory getMyRequestFactory(){
        return (TimeoutHttpComponentsClientHttpRequestFactory) this.getRequestFactory();
    }

}
