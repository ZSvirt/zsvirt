package org.zstack.sns.platform.feishu;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(path = "/sns/application-endpoints/feishu/at-persons",
        method = HttpMethod.POST,
        responseClass = APIAddSNSFeiShuAtPersonEvent.class,
        parameterName = "params")
public class APIAddSNSFeiShuAtPersonMsg extends APICreateMessage implements SNSApplicationEndpointMessage {
    @APIParam(maxLength = 64)
    private String userId;
    @APIParam(resourceType = SNSFeiShuEndpointVO.class)
    private String endpointUuid;

    @APIParam(maxLength = 128, required = false)
    private String remark;

    public static APIAddSNSFeiShuAtPersonMsg __example__() {
        APIAddSNSFeiShuAtPersonMsg msg = new APIAddSNSFeiShuAtPersonMsg();
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
