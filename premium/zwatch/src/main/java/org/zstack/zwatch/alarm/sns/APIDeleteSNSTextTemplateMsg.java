package org.zstack.zwatch.alarm.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zwatch/alarms/sns/text-templates/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteSNSTextTemplateEvent.class
)
public class APIDeleteSNSTextTemplateMsg extends APIDeleteMessage implements SNSTextTemplateMessage {
    @APIParam(resourceType = SNSTextTemplateVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public static APIDeleteSNSTextTemplateMsg __example__() {
        APIDeleteSNSTextTemplateMsg ret = new APIDeleteSNSTextTemplateMsg();
        ret.uuid = uuid();
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAlarmTextTemplateUuid() {
        return uuid;
    }
}
