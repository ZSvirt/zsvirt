package org.zstack.sns.platform.feishu;

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
import org.zstack.sns.platform.dingtalk.DingTalkMessageMetadata;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class SNSFeiShuEndpoint extends SNSApplicationEndpointBase {
    private static final CLogger logger = Utils.getLogger(SNSFeiShuEndpoint.class);

    @Autowired
    private RESTFacade restf;

    @Autowired
    private DatabaseFacade dbf;

    protected SNSFeiShuEndpointVO getSelf() {
        return (SNSFeiShuEndpointVO) self;
    }

    public SNSFeiShuEndpoint() {
    }

    public SNSFeiShuEndpoint(SNSApplicationEndpointVO self) {
        super(self);
    }

    protected void deleteHook() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(SNSFeiShuAtPersonVO.class).eq(SNSFeiShuAtPersonVO_.endpointUuid, self.getUuid()).hardDelete();
            }
        }.execute();
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddSNSFeiShuAtPersonMsg) {
            handle((APIAddSNSFeiShuAtPersonMsg) msg);
        } else if (msg instanceof APIRemoveSNSFeiShuAtPersonMsg) {
            handle((APIRemoveSNSFeiShuAtPersonMsg) msg);
        } else if (msg instanceof APISNSFeiShuTestConnectionMsg) {
            handle((APISNSFeiShuTestConnectionMsg) msg);
        } else if (msg instanceof APIUpdateAtPersonOfAtFeiShuEndpointMsg) {
            handle((APIUpdateAtPersonOfAtFeiShuEndpointMsg) msg);
        } else if (msg instanceof APIUpdateSNSFeiShuEndpointMsg) {
            handle((APIUpdateSNSFeiShuEndpointMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateSNSFeiShuEndpointMsg msg) {
        SNSFeiShuEndpointVO vo = dbf.findByUuid(msg.getUuid(), SNSFeiShuEndpointVO.class);

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
        evt.setInventory(SNSFeiShuEndpointInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIUpdateAtPersonOfAtFeiShuEndpointMsg msg) {
        SNSFeiShuAtPersonVO vo = dbf.findByUuid(msg.getUuid(), SNSFeiShuAtPersonVO.class);

        if (StringUtils.isNotBlank(msg.getUserId())) {
            vo.setUserId(msg.getUserId());
        }
        if (msg.getRemark() != null) {
            vo.setRemark(msg.getRemark());
        }

        dbf.update(vo);

        APIUpdateAtPersonOfFeiShuEndpointEvent evt = new APIUpdateAtPersonOfFeiShuEndpointEvent(msg.getId());
        evt.setInventory(SNSFeiShuAtPersonInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APISNSFeiShuTestConnectionMsg msg) {
        String url = msg.getUrl();
        Boolean atAll = msg.getAtAll();
        List<String> atPersonUserIds = msg.getAtPersonUserIds();
        String testMsg = msg.getTestMsg();
        String secret = msg.getSecret();

        if (StringUtils.isNotBlank(msg.getEndpointUuid())) {
            SNSFeiShuEndpointVO endpointVO = dbf.findByUuid(msg.getEndpointUuid(), SNSFeiShuEndpointVO.class);
            url = endpointVO.getUrl();
            atAll = endpointVO.isAtAll();
            atPersonUserIds = endpointVO.getAtPersons().stream()
                    .map(SNSFeiShuAtPersonVO::getUserId)
                    .collect(Collectors.toList());
            secret = endpointVO.getSecret();
        }

        StringBuilder sb = null;
        if (atAll != null && atAll) {
            sb = new StringBuilder(testMsg);
            sb.append("\n\n");
            sb.append("<at user_id=\"all\">all</at>");

            testMsg = sb.toString();
        } else if (atPersonUserIds != null && !atPersonUserIds.isEmpty()) {
            sb = new StringBuilder(testMsg);
            sb.append("\n\n");

            for (String userId : atPersonUserIds)
                sb.append("<at user_id=\"").append(userId).append("\"></at>");
        }
        if (sb != null) testMsg = sb.toString();

        FeiShuMessage message = new FeiShuMessage();
        message.content = new FeiShuMessage.Text(testMsg);
        if (StringUtils.isNotBlank(secret)) {
            int timestamp = Math.toIntExact(Instant.now().getEpochSecond());
            String sign = feiShuGenSign(secret, timestamp);
            message.timestamp = timestamp;
            message.sign = sign;
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

        APISNSFeiShuTestConnectionEvent evt = new APISNSFeiShuTestConnectionEvent(msg.getId());
        evt.setConnected(rsp.getStatusCode().is2xxSuccessful());
        evt.setWebhookResp(JSONObjectUtil.toObject(rsp.getBody(), LinkedHashMap.class));
        bus.publish(evt);
    }

    private void handle(APIAddSNSFeiShuAtPersonMsg msg) {
        SNSFeiShuAtPersonVO vo = new SNSFeiShuAtPersonVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setUserId(msg.getUserId());
        vo.setEndpointUuid(msg.getEndpointUuid());
        vo.setRemark(msg.getRemark());
        vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        dbf.persist(vo);

        APIAddSNSFeiShuAtPersonEvent evt = new APIAddSNSFeiShuAtPersonEvent(msg.getId());
        evt.setInventory(SNSFeiShuAtPersonInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIRemoveSNSFeiShuAtPersonMsg msg) {
        SQL.New(SNSFeiShuAtPersonVO.class)
                .eq(SNSFeiShuAtPersonVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSFeiShuAtPersonVO_.userId, msg.getUserId())
                .hardDelete();

        APIRemoveSNSFeiShuAtPersonEvent evt = new APIRemoveSNSFeiShuAtPersonEvent(msg.getId());
        bus.publish(evt);
    }

    @Override
    public void publish(MessageStruct message, Completion completion) {
        if (message.getMessage() == null || message.getMetadata() == null) {
            logger.debug("SNS message field not defined for FeiShu");
            completion.success();
            return;
        }

        FeiShuMessage msg = new FeiShuMessage();
        FeiShuMessageMetadata metadata = JSONObjectUtil.rehashObject(message.getMetadata(), FeiShuMessageMetadata.class);
        StringBuilder builder = new StringBuilder();
        builder.append(metadata.getTitle());
        builder.append("\n");
        builder.append(message.getMessage());
        builder.append("\n\n");

        if (getSelf().isAtAll()) {
            msg.at.isAtAll = true;
            builder.append("<at user_id=\"all\">all</at>");
            message.setMessage(builder.toString());
        } else if (!getSelf().getAtPersons().isEmpty()) {
            msg.at.atUserIds = getSelf().getAtPersons().stream().map(SNSFeiShuAtPersonVO::getUserId).collect(Collectors.toList());
            msg.at.atUserIds.forEach(userId -> builder.append("<at user_id=\"").append(userId).append("\">").append(userId).append("</at>"));
            message.setMessage(builder.toString());
        }

        msg.content = new FeiShuMessage.Text(builder.toString());
        if (StringUtils.isNotBlank(getSelf().getSecret())) {
            int timestamp = Math.toIntExact(Instant.now().getEpochSecond());
            msg.timestamp = timestamp;
            msg.sign = feiShuGenSign(getSelf().getSecret(), timestamp);
        }

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
            completion.fail(operr("failed to send messages to FeiShu. status: %s, body: %s", rsp.getStatusCode(), rsp.getBody()));
        }
    }

    private String feiShuGenSign(String secret, int timestamp) {
        try {
            // timestamp+"\n"+ key as the signature string
            String stringToSign = timestamp + "\n" + secret;
            // The signature is computed using the Hmac SHA 256 algorithm
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[]{});
            return new String(Base64.encodeBase64(signData));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CloudRuntimeException("GenSign error" + e.getMessage());
        }
    }
}
