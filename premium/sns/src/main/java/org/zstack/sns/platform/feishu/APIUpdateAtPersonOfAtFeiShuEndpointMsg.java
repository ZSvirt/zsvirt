package org.zstack.sns.platform.feishu;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(
        path = "/sns/application-endpoints/feishu/at-persons/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateAtPersonOfFeiShuEndpointEvent.class,
        isAction = true
)
public class APIUpdateAtPersonOfAtFeiShuEndpointMsg extends APIMessage implements SNSApplicationEndpointMessage  {
    @APIParam(resourceType = SNSFeiShuAtPersonVO.class)
    private String uuid;
    @APIParam(resourceType = SNSFeiShuEndpointVO.class)
    private String endpointUuid;
    @APIParam(maxLength = 256, required = false)
    private String userId;

    @APIParam(maxLength = 128, required = false)
    private String remark;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public static APIUpdateAtPersonOfAtFeiShuEndpointMsg __example__() {
        APIUpdateAtPersonOfAtFeiShuEndpointMsg msg = new APIUpdateAtPersonOfAtFeiShuEndpointMsg();
        msg.setUuid(uuid());
        msg.setEndpointUuid(uuid());
        msg.setUserId("zhang.san");
        msg.setRemark("jack");
        return msg;
    }

    @Override
    public String getApplicationEndpointUuid() {
        return endpointUuid;
    }
}
