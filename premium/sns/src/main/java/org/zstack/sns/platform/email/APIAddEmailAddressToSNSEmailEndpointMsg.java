package org.zstack.sns.platform.email;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(
        path = "/sns/application-endpoints/emails/email-addresses",
        method = HttpMethod.POST,
        responseClass = APIAddEmailAddressToSNSEmailEndpointEvent.class,
        parameterName = "params"
)
public class APIAddEmailAddressToSNSEmailEndpointMsg extends APICreateMessage implements SNSApplicationEndpointMessage {
    @APIParam(maxLength = 1024)
    private String emailAddress;
    @APIParam(resourceType = SNSEmailEndpointVO.class)
    private String endpointUuid;

    public static APIAddEmailAddressToSNSEmailEndpointMsg __example__() {
        APIAddEmailAddressToSNSEmailEndpointMsg msg = new APIAddEmailAddressToSNSEmailEndpointMsg();
        msg.setEmailAddress("example@zstack.io");
        msg.setEndpointUuid(uuid(SNSEmailEndpointVO.class));
        return msg;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    @Override
    public String getApplicationEndpointUuid() {
        return endpointUuid;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
