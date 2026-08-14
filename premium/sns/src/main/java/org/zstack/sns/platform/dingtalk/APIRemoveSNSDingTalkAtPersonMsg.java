package org.zstack.sns.platform.dingtalk;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(path = "/sns/application-endpoints/ding-talk/{endpointUuid}/at-persons/{phoneNumber}", method = HttpMethod.DELETE, responseClass = APIRemoveSNSDingTalkAtPersonEvent.class)
public class APIRemoveSNSDingTalkAtPersonMsg extends APIDeleteMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSDingTalkEndpointVO.class, successIfResourceNotExisting = true)
    private String endpointUuid;
    @APIParam
    private String phoneNumber;

    public static APIRemoveSNSDingTalkAtPersonMsg __example__() {
        APIRemoveSNSDingTalkAtPersonMsg msg = new APIRemoveSNSDingTalkAtPersonMsg();
        msg.endpointUuid = uuid();
        msg.phoneNumber = "18988887777";
        return msg;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
}
