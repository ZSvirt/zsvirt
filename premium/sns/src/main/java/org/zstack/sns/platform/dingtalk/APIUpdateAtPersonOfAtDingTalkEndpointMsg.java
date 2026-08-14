package org.zstack.sns.platform.dingtalk;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(
        path = "/sns/application-endpoints/ding-talk/at-persons/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateAtPersonOfDingTalkEndpointEvent.class,
        isAction = true
)
public class APIUpdateAtPersonOfAtDingTalkEndpointMsg extends APIMessage implements SNSApplicationEndpointMessage  {

    @APIParam(resourceType = SNSDingTalkAtPersonVO.class)
    private String uuid;

    @APIParam(resourceType = SNSDingTalkEndpointVO.class)
    private String endpointUuid;

    @APIParam(maxLength = 256, required = false)
    private String phoneNumber;

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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public static APIUpdateAtPersonOfAtDingTalkEndpointMsg __example__() {
        APIUpdateAtPersonOfAtDingTalkEndpointMsg msg = new APIUpdateAtPersonOfAtDingTalkEndpointMsg();
        msg.setUuid(uuid());
        msg.setEndpointUuid(uuid());
        msg.setPhoneNumber("13062689903");
        msg.setRemark("jack");
        return msg;
    }

    @Override
    public String getApplicationEndpointUuid() {
        return endpointUuid;
    }
}
