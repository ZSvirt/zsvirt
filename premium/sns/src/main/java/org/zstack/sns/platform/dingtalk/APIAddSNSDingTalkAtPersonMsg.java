package org.zstack.sns.platform.dingtalk;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointMessage;

@RestRequest(path = "/sns/application-endpoints/ding-talk/at-persons",
        method = HttpMethod.POST,
        responseClass = APIAddSNSDingTalkAtPersonEvent.class,
        parameterName = "params")
public class APIAddSNSDingTalkAtPersonMsg extends APICreateMessage implements SNSApplicationEndpointMessage {
    @APIParam(maxLength = 64)
    private String phoneNumber;
    @APIParam(resourceType = SNSDingTalkEndpointVO.class)
    private String endpointUuid;

    @APIParam(maxLength = 128, required = false)
    private String remark;

    public static APIAddSNSDingTalkAtPersonMsg __example__() {
        APIAddSNSDingTalkAtPersonMsg msg = new APIAddSNSDingTalkAtPersonMsg();
        msg.setPhoneNumber("18099997777");
        msg.setEndpointUuid(uuid());
        msg.setRemark("jack");
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
