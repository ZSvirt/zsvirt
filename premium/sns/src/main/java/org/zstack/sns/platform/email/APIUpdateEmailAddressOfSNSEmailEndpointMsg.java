package org.zstack.sns.platform.email;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(
        path = "/sns/application-endpoints/emails/email-addresses",
        method = HttpMethod.PUT,
        responseClass = APIUpdateEmailAddressOfSNSEmailEndpointEvent.class,
        isAction = true
)
public class APIUpdateEmailAddressOfSNSEmailEndpointMsg extends APIMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSEmailAddressVO.class)
    private String emailAddressUuid;
    @APIParam(resourceType = SNSEmailEndpointVO.class)
    private String endpointUuid;
    @APIParam(maxLength = 1024)
    private String emailAddress;

    public String getEmailAddressUuid() {
        return emailAddressUuid;
    }

    public void setEmailAddressUuid(String emailAddressUuid) {
        this.emailAddressUuid = emailAddressUuid;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public String getApplicationEndpointUuid() {
        return endpointUuid;
    }

    public static APIUpdateEmailAddressOfSNSEmailEndpointMsg __example__() {
        APIUpdateEmailAddressOfSNSEmailEndpointMsg msg = new APIUpdateEmailAddressOfSNSEmailEndpointMsg();
        msg.setEmailAddress("example@zstack.io");
        msg.setEndpointUuid(uuid(SNSEmailEndpointVO.class));
        msg.setEmailAddressUuid(uuid(SNSEmailAddressVO.class));
        return msg;
    }

}
