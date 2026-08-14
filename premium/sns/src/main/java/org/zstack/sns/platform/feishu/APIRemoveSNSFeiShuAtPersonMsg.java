package org.zstack.sns.platform.feishu;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(path = "/sns/application-endpoints/feishu/{endpointUuid}/at-persons/{userId}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveSNSFeiShuAtPersonEvent.class)
public class APIRemoveSNSFeiShuAtPersonMsg extends APIDeleteMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSFeiShuEndpointVO.class, successIfResourceNotExisting = true)
    private String endpointUuid;
    @APIParam
    private String userId;

    public static APIRemoveSNSFeiShuAtPersonMsg __example__() {
        APIRemoveSNSFeiShuAtPersonMsg msg = new APIRemoveSNSFeiShuAtPersonMsg();
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
