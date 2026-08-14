package org.zstack.sns.platform.microsoftteams;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.header.core.Completion;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RESTFacade;
import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent;
import org.zstack.sns.MessageStruct;
import org.zstack.sns.SNSApplicationEndpointBase;
import org.zstack.sns.SNSApplicationEndpointVO;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.map;

public class SNSMicrosoftTeamsEndpoint extends SNSApplicationEndpointBase {
    private static final CLogger logger = Utils.getLogger(SNSMicrosoftTeamsEndpoint.class);

    @Autowired
    private RESTFacade restf;

    @Autowired
    private DatabaseFacade dbf;

    protected SNSMicrosoftTeamsEndpointVO getSelf() {
        return (SNSMicrosoftTeamsEndpointVO) self;
    }

    public SNSMicrosoftTeamsEndpoint() {
    }

    public SNSMicrosoftTeamsEndpoint(SNSApplicationEndpointVO self) {
        super(self);
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APISNSMicrosoftTeamsTestConnectionMsg) {
            handle((APISNSMicrosoftTeamsTestConnectionMsg) msg);
        } else if (msg instanceof APIUpdateSNSMicrosoftTeamsEndpointMsg) {
            handle((APIUpdateSNSMicrosoftTeamsEndpointMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateSNSMicrosoftTeamsEndpointMsg msg) {
        SNSMicrosoftTeamsEndpointVO vo = dbf.findByUuid(msg.getUuid(), SNSMicrosoftTeamsEndpointVO.class);

        if (StringUtils.isNotBlank(msg.getName())) {
            vo.setName(msg.getName());
        }
        if (StringUtils.isNotBlank(msg.getDescription())) {
            vo.setDescription(msg.getDescription());
        }
        if (StringUtils.isNotBlank(msg.getUrl())) {
            vo.setUrl(msg.getUrl());
        }

        vo = dbf.updateAndRefresh(vo);

        APIUpdateSNSApplicationEndpointEvent evt = new APIUpdateSNSApplicationEndpointEvent(msg.getId());
        evt.setInventory(SNSMicrosoftTeamsEndpointInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APISNSMicrosoftTeamsTestConnectionMsg msg) {
        String url = msg.getUrl();
        String testMsg = msg.getTestMsg();

        if (StringUtils.isNotBlank(msg.getEndpointUuid())) {
            SNSMicrosoftTeamsEndpointVO endpointVO
                    = dbf.findByUuid(msg.getEndpointUuid(), SNSMicrosoftTeamsEndpointVO.class);
            url = endpointVO.getUrl();
        }

        MicrosoftTeamsMessage message = new MicrosoftTeamsMessage();
        message.teamsJson = new MicrosoftTeamsMessage.TeamsJson(testMsg);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json;charset=utf-8");
        HttpEntity<String> req = new HttpEntity<>(JSONObjectUtil.toJsonString(message.teamsJson), headers);
        final String requestUrl = url;
        ResponseEntity<String> rsp = new Retry<ResponseEntity<String>>() {
            @Override
            @RetryCondition(onExceptions = {IOException.class, HttpStatusCodeException.class})
            protected ResponseEntity<String> call() {
                return restf.getRESTTemplate().exchange(requestUrl, HttpMethod.POST, req, String.class);
            }
        }.run();

        APISNSMicrosoftTeamsTestConnectionEvent evt = new APISNSMicrosoftTeamsTestConnectionEvent(msg.getId());
        evt.setConnected(rsp.getStatusCode().is2xxSuccessful());
        // ms req success, return "1", no json string
        evt.setWebhookResp(new LinkedHashMap<>(map(CollectionDSL.e("ms_result", rsp.getBody()))));
        bus.publish(evt);
    }

    @Override
    public void publish(MessageStruct message, Completion completion) {
        if (message.getMessage() == null || message.getMetadata() == null) {
            logger.debug("SNS message field not defined for Microsoft Teams");
            completion.success();
            return;
        }

        MicrosoftTeamsMessageMetadata metadata = JSONObjectUtil.rehashObject(message.getMetadata(), MicrosoftTeamsMessageMetadata.class);
        MicrosoftTeamsMessage msg = new MicrosoftTeamsMessage();
        msg.teamsJson = new MicrosoftTeamsMessage.TeamsJson(message.getMessage(), metadata.getTitle());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json;charset=utf-8");
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
            completion.fail(operr("failed to send messages to Microsoft Teams. status: %s, body: %s", rsp.getStatusCode(), rsp.getBody()));
        }
    }
}
