package org.zstack.sns.platform.email;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointTestConnectionMessage;
import org.zstack.sns.SNSApplicationEndpointType;

import java.util.Collections;
import java.util.List;

@RestRequest(path = "/sns/application-endpoints/email/test-connection",
        method = HttpMethod.POST,
        responseClass = APISNSEmailTestConnectionEvent.class,
        parameterName = "params")
public class APISNSEmailTestConnectionMsg extends APIMessage implements SNSApplicationEndpointTestConnectionMessage {

    @APIParam(required = false)
    private List<String> emails;

    @APIParam(required = false, resourceType = SNSEmailPlatformVO.class)
    private String platformUuid;

    @APIParam(required = false, resourceType = SNSEmailEndpointVO.class)
    private String endpointUuid;

    @APIParam(required = false, maxLength = 512)
    private String subject;

    @APIParam(required = false, maxLength = 8192)
    private String text;

    public static APISNSEmailTestConnectionMsg __example__() {
        APISNSEmailTestConnectionMsg msg = new APISNSEmailTestConnectionMsg();
        msg.setEmails(Collections.singletonList("example@zstack.io"));
        msg.setPlatformUuid(uuid());
        msg.setEndpointUuid(uuid());
        msg.setSubject("test email subject");
        msg.setText("this is a test email of content");
        return msg;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public String getPlatformUuid() {
        return platformUuid;
    }

    public void setPlatformUuid(String platformUuid) {
        this.platformUuid = platformUuid;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public SNSApplicationEndpointType getEndpointType() {
        return SNSEmailPlatformFactory.endpointType;
    }
}
