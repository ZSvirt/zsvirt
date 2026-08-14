package org.zstack.sns.platform.http;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.header.core.Completion;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.sns.*;
import org.zstack.sns.system.SNSSystemAlarmTopicManager;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class SNSHttpEndpoint extends SNSApplicationEndpointBase {
    @Autowired
    private RESTFacade restf;

    protected SNSHttpEndpointVO getSelf() {
        return (SNSHttpEndpointVO) self;
    }

    public SNSHttpEndpoint() {}

    public SNSHttpEndpoint(SNSApplicationEndpointVO self) {
        super(self);
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateSNSHttpEndpointMsg) {
            handle((APIUpdateSNSHttpEndpointMsg) msg);
        } else if (msg instanceof APISNSHttpTestConnectionMsg) {
            handle((APISNSHttpTestConnectionMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APISNSHttpTestConnectionMsg msg) {
        String url = msg.getUrl();
        String username = msg.getUsername();
        String password = msg.getPassword();

        if (StringUtils.isNotBlank(msg.getEndpointUuid())) {
            SNSHttpEndpointVO endpointVO = dbf.findByUuid(msg.getEndpointUuid(), SNSHttpEndpointVO.class);
            url = endpointVO.getUrl();
            username = endpointVO.getUsername();
            password = endpointVO.getPassword();
        }

        String alarmHttpTemplate = "{\n" +
                "    \"product\": \"zstack\",\n" +
                "    \"service\": \"zstack\",\n" +
                "    \"metric\": \"ZStack::CPU\",\n" +
                "    \"alertLevel\": 1,\n" +
                "    \"alertTime\": \"1111\",\n" +
                "    \"dimensions\": \"aaa\",\n" +
                "    \"message\": \"CPU >= 100\",\n" +
                "    \"dataSource\": \"zstack\"\n" +
                "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(RESTConstant.APP_JSON_UTF8));
        headers.setContentLength(JSONObjectUtil.toJsonString(alarmHttpTemplate).length());

        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String( encodedAuth );
            headers.add( "Authorization", authHeader );
        }

        HttpEntity<String> req = new HttpEntity<>(JSONObjectUtil.toJsonString(alarmHttpTemplate), headers);
        String requestUrl = url;
        ResponseEntity<String> rsp = new Retry<ResponseEntity<String>>() {
            @Override
            @RetryCondition(onExceptions = {IOException.class, HttpStatusCodeException.class})
            protected ResponseEntity<String> call() {
                return restf.getRESTTemplate().exchange(requestUrl, HttpMethod.POST, req, String.class);
            }
        }.run();

        APISNSHttpTestConnectionEvent evt = new APISNSHttpTestConnectionEvent(msg.getId());
        evt.setConnected(rsp.getStatusCode().is2xxSuccessful());
        evt.setWebhookResp(rsp.getBody());
        bus.publish(evt);
    }

    private void handle(APIUpdateSNSHttpEndpointMsg msg) {
        SNSHttpEndpointVO vo  = dbf.findByUuid(msg.getUuid(), SNSHttpEndpointVO.class);

        if (StringUtils.isNotBlank(msg.getName())) {
            vo.setName(msg.getName());
        }
        if (StringUtils.isNotBlank(msg.getDescription())) {
            vo.setDescription(msg.getDescription());
        }
        if (StringUtils.isNotBlank(msg.getUrl())) {
            vo.setUrl(msg.getUrl());
        }
        if (StringUtils.isNotBlank(msg.getUsername())) {
            vo.setUsername(msg.getUsername());
        }
        if (StringUtils.isNotBlank(msg.getPassword())) {
            vo.setPassword(msg.getPassword());
        }

        vo = dbf.updateAndRefresh(vo);

        APIUpdateSNSApplicationEndpointEvent evt = new APIUpdateSNSApplicationEndpointEvent(msg.getId());
        evt.setInventory(SNSHttpEndpointInventory.valueOf(vo));
        bus.publish(evt);
    }

    @Override
    public void publish(MessageStruct message, Completion completion) {
        if (StringUtils.isEmpty(getSelf().getUrl()) &&
                (SNSApplicationEndpointOwnerType.System == self.getOwnerType() || self.getName().equals(SNSSystemAlarmTopicManager.SYSTEM_ALARM_ENDPOINT_NAME))) {
            completion.success();
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(RESTConstant.APP_JSON_UTF8));
        headers.setContentLength(message.getMessage().length());
        if (message.getMetadata() != null) {
            message.getMetadata().forEach((k, v) -> headers.add((String)k, v.toString()));
        }

        if (getSelf().getUsername() != null && getSelf().getPassword() != null) {
            String auth = getSelf().getUsername() + ":" + getSelf().getPassword();
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")) );
            String authHeader = "Basic " + new String( encodedAuth );
            headers.add( "Authorization", authHeader );
        }

        HttpEntity<String> req = new HttpEntity<>(message.getMessage(), headers);
        ResponseEntity<String> rsp = new Retry<ResponseEntity<String>>() {
            @Override
            @RetryCondition(onExceptions = {IOException.class, HttpStatusCodeException.class})
            protected ResponseEntity<String> call() {
                return restf.getRESTTemplate().exchange(getSelf().getUrl(), HttpMethod.POST, req, String.class);
            }
        }.run();

        if (rsp.getStatusCode().is2xxSuccessful()) {
            completion.success();
        } else {
            completion.fail(operr("HTTP POST failure. status: %s, body: %s", rsp.getStatusCode(), rsp.getBody()));
        }
    }
}
