package org.zstack.sns.platform.wecom;

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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class SNSWeComEndpoint extends SNSApplicationEndpointBase {
    private static final CLogger logger = Utils.getLogger(SNSWeComEndpoint.class);

    @Autowired
    private RESTFacade restf;

    @Autowired
    private DatabaseFacade dbf;

    protected SNSWeComEndpointVO getSelf() {
        return (SNSWeComEndpointVO) self;
    }

    public SNSWeComEndpoint() {
    }

    public SNSWeComEndpoint(SNSApplicationEndpointVO self) {
        super(self);
    }

    protected void deleteHook() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(SNSWeComAtPersonVO.class).eq(SNSWeComAtPersonVO_.endpointUuid, self.getUuid()).hardDelete();
            }
        }.execute();
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddSNSWeComAtPersonMsg) {
            handle((APIAddSNSWeComAtPersonMsg) msg);
        } else if (msg instanceof APIRemoveSNSWeComAtPersonMsg) {
            handle((APIRemoveSNSWeComAtPersonMsg) msg);
        } else if (msg instanceof APISNSWeComTestConnectionMsg) {
            handle((APISNSWeComTestConnectionMsg) msg);
        } else if (msg instanceof APIUpdateAtPersonOfAtWeComEndpointMsg) {
            handle((APIUpdateAtPersonOfAtWeComEndpointMsg) msg);
        } else if (msg instanceof APIUpdateSNSWeComEndpointMsg) {
            handle((APIUpdateSNSWeComEndpointMsg) msg);
        }else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateSNSWeComEndpointMsg msg) {
        SNSWeComEndpointVO vo = dbf.findByUuid(msg.getUuid(), SNSWeComEndpointVO.class);

        if (StringUtils.isNotBlank(msg.getName())) {
            vo.setName(msg.getName());
        }
        if (StringUtils.isNotBlank(msg.getDescription())) {
            vo.setDescription(msg.getDescription());
        }
        if (StringUtils.isNotBlank(msg.getUrl())) {
            vo.setUrl(msg.getUrl());
        }
        if (msg.getAtAll() != null) {
            vo.setAtAll(msg.getAtAll());
        }

        vo = dbf.updateAndRefresh(vo);

        APIUpdateSNSApplicationEndpointEvent evt = new APIUpdateSNSApplicationEndpointEvent(msg.getId());
        evt.setInventory(SNSWeComEndpointInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIUpdateAtPersonOfAtWeComEndpointMsg msg) {
        SNSWeComAtPersonVO vo = dbf.findByUuid(msg.getUuid(), SNSWeComAtPersonVO.class);

        if (StringUtils.isNotBlank(msg.getUserId())) {
            vo.setUserId(msg.getUserId());
        }
        if (msg.getRemark() != null) {
            vo.setRemark(msg.getRemark());
        }

        dbf.update(vo);

        APIUpdateAtPersonOfWeComEndpointEvent evt = new APIUpdateAtPersonOfWeComEndpointEvent(msg.getId());
        evt.setInventory(SNSWeComAtPersonInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APISNSWeComTestConnectionMsg msg) {
        String url = msg.getUrl();
        Boolean atAll = msg.getAtAll();
        List<String> atPersonUserIds = msg.getAtPersonUserIds();
        String testMsg = msg.getTestMsg();

        if (StringUtils.isNotBlank(msg.getEndpointUuid())) {
            SNSWeComEndpointVO endpointVO = dbf.findByUuid(msg.getEndpointUuid(), SNSWeComEndpointVO.class);
            url = endpointVO.getUrl();
            atAll = endpointVO.isAtAll();
            atPersonUserIds = endpointVO.getAtPersons().stream()
                    .map(SNSWeComAtPersonVO::getUserId)
                    .collect(Collectors.toList());
        }

        StringBuilder sb = null;
        if (atAll != null && atAll) {
            sb = new StringBuilder(testMsg);
            sb.append("\n\n");
            sb.append("<@all>");

            testMsg = sb.toString();
        } else if (atPersonUserIds != null && !atPersonUserIds.isEmpty()) {
            sb = new StringBuilder(testMsg);
            sb.append("\n\n");

            for (String userId : atPersonUserIds)
                sb.append("<@").append(userId).append(">");
        }
        if (sb != null) testMsg = sb.toString();

        WeComMessage message = new WeComMessage();
        message.markdown = new WeComMessage.Markdown(testMsg);

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

        APISNSWeComTestConnectionEvent evt = new APISNSWeComTestConnectionEvent(msg.getId());
        evt.setConnected(true);
        evt.setWebhookResp(JSONObjectUtil.toObject(rsp.getBody(), LinkedHashMap.class));
        bus.publish(evt);
    }

    private void handle(APIAddSNSWeComAtPersonMsg msg) {
        SNSWeComAtPersonVO vo = new SNSWeComAtPersonVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setUserId(msg.getUserId());
        vo.setEndpointUuid(msg.getEndpointUuid());
        vo.setRemark(msg.getRemark());
        vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        dbf.persist(vo);

        APIAddSNSWeComAtPersonEvent evt = new APIAddSNSWeComAtPersonEvent(msg.getId());
        evt.setInventory(SNSWeComAtPersonInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIRemoveSNSWeComAtPersonMsg msg) {
        SQL.New(SNSWeComAtPersonVO.class).eq(SNSWeComAtPersonVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSWeComAtPersonVO_.userId, msg.getUserId()).hardDelete();

        APIRemoveSNSWeComAtPersonEvent evt = new APIRemoveSNSWeComAtPersonEvent(msg.getId());
        bus.publish(evt);
    }

    @Override
    public void publish(MessageStruct message, Completion completion) {
        if (message.getMessage() == null || message.getMetadata() == null) {
            logger.debug("SNS message field not defined for WeCom");
            completion.success();
            return;
        }

        WeComMessage msg = new WeComMessage();
        WeComMessageMetadata metadata = JSONObjectUtil.rehashObject(message.getMetadata(), WeComMessageMetadata.class);
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(metadata.getTitle());
        builder.append("\n");
        builder.append(message.getMessage());
        builder.append("\n\n");

        if (getSelf().isAtAll()) {
            msg.at.isAtAll = true;
            builder.append("<@all>");
            message.setMessage(builder.toString());
        } else if (!getSelf().getAtPersons().isEmpty()) {
            msg.at.atUserIds = getSelf().getAtPersons().stream().map(SNSWeComAtPersonVO::getUserId).collect(Collectors.toList());
            msg.at.atUserIds.forEach(userId -> builder.append("<@").append(userId).append(">"));
            message.setMessage(builder.toString());
        }

        msg.markdown = new WeComMessage.Markdown(builder.toString());

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
            completion.fail(operr("failed to send messages to WeCom. status: %s, body: %s", rsp.getStatusCode(), rsp.getBody()));
        }
    }
}
