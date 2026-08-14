package org.zstack.sns.platform.wecom;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(path = "/sns/application-endpoints/we-com/at-persons",
        method = HttpMethod.POST,
        responseClass = APIAddSNSWeComAtPersonEvent.class,
        parameterName = "params")
public class APIAddSNSWeComAtPersonMsg extends APICreateMessage implements SNSApplicationEndpointMessage {
    @APIParam(maxLength = 64)
    private String userId;
    @APIParam(resourceType = SNSWeComEndpointVO.class)
    private String endpointUuid;

    @APIParam(maxLength = 128, required = false)
    private String remark;

    public static APIAddSNSWeComAtPersonMsg __example__() {
        APIAddSNSWeComAtPersonMsg msg = new APIAddSNSWeComAtPersonMsg();
        msg.setUserId("18099997777");
        msg.setEndpointUuid(uuid());
        msg.setRemark("jack");
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
