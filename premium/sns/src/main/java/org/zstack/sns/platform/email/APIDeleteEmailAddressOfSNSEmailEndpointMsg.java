package org.zstack.sns.platform.email;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(
        path = "/sns/application-endpoints/emails/{endpointUuid}/email-addresses/{emailAddressUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteEmailAddressOfSNSEmailEndpointEvent.class
)
public class APIDeleteEmailAddressOfSNSEmailEndpointMsg extends APIMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSEmailAddressVO.class, successIfResourceNotExisting = true)
    private String emailAddressUuid;
    @APIParam(resourceType = SNSEmailEndpointVO.class)
    private String endpointUuid;

    public static APIDeleteEmailAddressOfSNSEmailEndpointMsg __example__() {
        APIDeleteEmailAddressOfSNSEmailEndpointMsg msg = new APIDeleteEmailAddressOfSNSEmailEndpointMsg();
        msg.setEmailAddressUuid(uuid());
        msg.setEndpointUuid(uuid());
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

    public String getEmailAddressUuid() {
        return emailAddressUuid;
    }

    public void setEmailAddressUuid(String emailAddressUuid) {
        this.emailAddressUuid = emailAddressUuid;
    }
}