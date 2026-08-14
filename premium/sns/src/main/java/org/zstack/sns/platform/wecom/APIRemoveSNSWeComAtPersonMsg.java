package org.zstack.sns.platform.wecom;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(path = "/sns/application-endpoints/we-com/{endpointUuid}/at-persons/{userId}", method = HttpMethod.DELETE, responseClass = APIRemoveSNSWeComAtPersonEvent.class)
public class APIRemoveSNSWeComAtPersonMsg extends APIDeleteMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSWeComEndpointVO.class, successIfResourceNotExisting = true)
    private String endpointUuid;
    @APIParam
    private String userId;

    public static APIRemoveSNSWeComAtPersonMsg __example__() {
        APIRemoveSNSWeComAtPersonMsg msg = new APIRemoveSNSWeComAtPersonMsg();
        msg.endpointUuid = uuid();
        msg.userId = "18988887777";
        return msg;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
