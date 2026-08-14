package org.zstack.core.webhook;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.header.core.webhooks.WebhookInventory;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2017/5/8.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class WebhookCaller {
    protected static CLogger logger = Utils.getLogger(WebhookCaller.class);

    protected static RestTemplate restTemplate = RESTFacade.createRestTemplate(
            (int)TimeUnit.SECONDS.toMillis(30),
            (int)TimeUnit.SECONDS.toMillis(30)
    );

    protected void postToWebhooks(List<WebhookInventory> hooks, String body) {
        for (WebhookInventory hook : hooks) {
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.valueOf(RESTConstant.APP_JSON_UTF8));
            requestHeaders.setContentLength(body.length());
            HttpEntity<String> req = new HttpEntity<String>(body, requestHeaders);

            ResponseEntity<String> rsp = new Retry<ResponseEntity<String>>() {
                @Override
                @RetryCondition(onExceptions = {IOException.class, RestClientException.class})
                protected ResponseEntity<String> call() {
                    return restTemplate.exchange(hook.getUrl(), HttpMethod.POST, req, String.class);
                }
            }.run();

            if (!rsp.getStatusCode().is2xxSuccessful()) {
                logger.warn(String.format("unable to call the webhook[uuid:%s, name:%s, url:%s], status code: %s, body: %s",
                        hook.getUuid(), hook.getName(), hook.getUrl(), rsp.getStatusCode(), rsp.getBody()));
            }
        }
    }

    public abstract void call();
}
