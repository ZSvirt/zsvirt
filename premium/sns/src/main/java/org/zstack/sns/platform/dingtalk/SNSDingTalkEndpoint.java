package org.zstack.sns.platform.dingtalk;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.header.core.Completion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RESTFacade;
import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent;
import org.zstack.sns.MessageStruct;
import org.zstack.sns.SNSApplicationEndpointBase;
import org.zstack.sns.SNSApplicationEndpointVO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class SNSDingTalkEndpoint extends SNSApplicationEndpointBase {
    private static final CLogger logger = Utils.getLogger(SNSDingTalkEndpoint.class);

    @Autowired
    private RESTFacade restf;

    @Autowired
    private DatabaseFacade dbf;

    protected SNSDingTalkEndpointVO getSelf() {
        return (SNSDingTalkEndpointVO) self;
    }

    public SNSDingTalkEndpoint() {
    }

    public SNSDingTalkEndpoint(SNSApplicationEndpointVO self) {
        super(self);
    }

    protected void deleteHook() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(SNSDingTalkAtPersonVO.class).eq(SNSDingTalkAtPersonVO_.endpointUuid, self.getUuid()).hardDelete();
            }
        }.execute();
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddSNSDingTalkAtPersonMsg) {
            handle((APIAddSNSDingTalkAtPersonMsg) msg);
        } else if (msg instanceof APIRemoveSNSDingTalkAtPersonMsg) {
            handle((APIRemoveSNSDingTalkAtPersonMsg) msg);
        } else if (msg instanceof APISNSDingTalkTestConnectionMsg) {
            handle((APISNSDingTalkTestConnectionMsg) msg);
        } else if (msg instanceof APIUpdateAtPersonOfAtDingTalkEndpointMsg) {
            handle((APIUpdateAtPersonOfAtDingTalkEndpointMsg) msg);
        } else if (msg instanceof APIUpdateSNSDingTalkEndpointMsg) {
            handle((APIUpdateSNSDingTalkEndpointMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateSNSDingTalkEndpointMsg msg) {
        SNSDingTalkEndpointVO vo = dbf.findByUuid(msg.getUuid(), SNSDingTalkEndpointVO.class);

        if (StringUtils.isNotBlank(msg.getName())) {
            vo.setName(msg.getName());
        }
        if (StringUtils.isNotBlank(msg.getDescription())) {
            vo.setDescription(msg.getDescription());
        }
        if (StringUtils.isNotBlank(msg.getUrl())) {
            vo.setUrl(msg.getUrl());
        }
        if (msg.getAtAll()!=null) {
            vo.setAtAll(msg.getAtAll());
        }
        // allow secret update empty string
        if (msg.getSecret() != null) {
            vo.setSecret(msg.getSecret());
        }

        vo = dbf.updateAndRefresh(vo);

        APIUpdateSNSApplicationEndpointEvent evt = new APIUpdateSNSApplicationEndpointEvent(msg.getId());
        evt.setInventory(SNSDingTalkEndpointInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIUpdateAtPersonOfAtDingTalkEndpointMsg msg) {
        SNSDingTalkAtPersonVO vo = dbf.findByUuid(msg.getUuid(), SNSDingTalkAtPersonVO.class);

        if (StringUtils.isNotBlank(msg.getPhoneNumber())) {
            vo.setPhoneNumber(msg.getPhoneNumber());
        }
        if (msg.getRemark() != null) {
            vo.setRemark(msg.getRemark());
        }
        dbf.update(vo);

        APIUpdateAtPersonOfDingTalkEndpointEvent evt = new APIUpdateAtPersonOfDingTalkEndpointEvent(msg.getId());
        evt.setInventory(SNSDingTalkAtPersonInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APISNSDingTalkTestConnectionMsg msg) {
        String url = msg.getUrl();
        Boolean atAll = msg.getAtAll();
        List<String> atPersonPhoneNumbers = msg.getAtPersonPhoneNumbers();
        String testMsg = msg.getTestMsg();
        String secret = msg.getSecret();

        if (StringUtils.isNotBlank(msg.getEndpointUuid())) {
            SNSDingTalkEndpointVO endpointVO = dbf.findByUuid(msg.getEndpointUuid(), SNSDingTalkEndpointVO.class);
            url = endpointVO.getUrl();
            atAll = endpointVO.isAtAll();
            atPersonPhoneNumbers = endpointVO.getAtPersons().stream()
                    .map(SNSDingTalkAtPersonVO::getPhoneNumber)
                    .collect(Collectors.toList());
            secret = endpointVO.getSecret();
        }

        StringBuilder sb = null;
        DingTalkMessage message = new DingTalkMessage();
        if (atAll != null && atAll) {
            message.at.isAtAll = true;
        } else if (atPersonPhoneNumbers != null && !atPersonPhoneNumbers.isEmpty()) {
            sb = new StringBuilder(testMsg);
            sb.append("\n\n");

            for (String userId : atPersonPhoneNumbers)
                sb.append("<at user_id=\"").append(userId).append("\"></at>");
        }
        if (sb != null) testMsg = sb.toString();

        message.markdown = new DingTalkMessage.Markdown(testMsg);
        if (StringUtils.isNotEmpty(secret)) {
            url = url + dingTalkGenSign(secret);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json;charset=utf-8");
        HttpEntity<String> req = new HttpEntity<>(JSONObjectUtil.toJsonString(message), headers);
        final String requestUrl = url;
        ResponseEntity<String> rsp = new Retry<ResponseEntity<String>>() {
            @Override
            @RetryCondition(onExceptions = {IOException.class, HttpStatusCodeException.class})
            protected ResponseEntity<String> call() {
                return restf.getRESTTemplate().exchange(requestUrl, HttpMethod.POST, req, String.class);
            }
        }.run();

        APISNSDingTalkTestConnectionEvent evt = new APISNSDingTalkTestConnectionEvent(msg.getId());
        evt.setConnected(rsp.getStatusCode().is2xxSuccessful());
        evt.setWebhookResp(JSONObjectUtil.toObject(rsp.getBody(), LinkedHashMap.class));
        bus.publish(evt);
    }

    private void handle(APIRemoveSNSDingTalkAtPersonMsg msg) {
        SQL.New(SNSDingTalkAtPersonVO.class).eq(SNSDingTalkAtPersonVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSDingTalkAtPersonVO_.phoneNumber, msg.getPhoneNumber()).hardDelete();

        APIRemoveSNSDingTalkAtPersonEvent evt = new APIRemoveSNSDingTalkAtPersonEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIAddSNSDingTalkAtPersonMsg msg) {
        SNSDingTalkAtPersonVO vo = new SNSDingTalkAtPersonVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setPhoneNumber(msg.getPhoneNumber());
        vo.setEndpointUuid(msg.getEndpointUuid());
        vo.setRemark(msg.getRemark());
        vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        dbf.persist(vo);

        APIAddSNSDingTalkAtPersonEvent evt = new APIAddSNSDingTalkAtPersonEvent(msg.getId());
        evt.setInventory(SNSDingTalkAtPersonInventory.valueOf(vo));
        bus.publish(evt);
    }

    @Override
    public void publish(MessageStruct message, Completion completion) {
        if (message.getMessage() == null || message.getMetadata() == null) {
            logger.debug("SNS message field not defined for DingTalk");
            completion.success();
            return;
        }

        DingTalkMessage msg = new DingTalkMessage();
        DingTalkMessageMetadata metadata = JSONObjectUtil.rehashObject(message.getMetadata(), DingTalkMessageMetadata.class);
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(metadata.getTitle());
        builder.append("\n");
        builder.append(message.getMessage());
        builder.append("\n\n");

        if (getSelf().isAtAll()) {
            msg.at.isAtAll = true;
        } else if (!getSelf().getAtPersons().isEmpty()) {
            msg.at.atMobiles = getSelf().getAtPersons().stream().map(SNSDingTalkAtPersonVO::getPhoneNumber).collect(Collectors.toList());
            msg.at.atMobiles.forEach(mobile -> {
                builder.append("@");
                builder.append(mobile);
            });


        }
        if (StringUtils.isNotEmpty(getSelf().getSecret())) {
            String url = getSelf().getUrl() + dingTalkGenSign(getSelf().getSecret());
            getSelf().setUrl(url);
        }

        message.setMessage(builder.toString());
        msg.markdown = new DingTalkMessage.Markdown(message.getMessage(), metadata.getTitle());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json;charset=utf-8");
        HttpEntity<String> req = new HttpEntity<>(JSONObjectUtil.toJsonString(msg), headers);
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
            completion.fail(operr("failed to send messages to DingTalk. status: %s, body: %s", rsp.getStatusCode(), rsp.getBody()));
        }
    }

    private String dingTalkGenSign(String secret) {
        try {
            // https://open.dingtalk.com/document/robots/customize-robot-security-settings
            Long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");
            return "&timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            throw new CloudRuntimeException((String.format("ding talk sign error %s", secret)));
        }
    }
}
