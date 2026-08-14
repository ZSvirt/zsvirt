package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by Qi Le on 2019-07-11
 */
@RestRequest(
        path = "/sns/sms-endpoints/receivers",
        method = HttpMethod.POST,
        responseClass = APIAddSNSSmsReceiverEvent.class,
        parameterName = "params"
)
public class APIAddSNSSmsReceiverMsg extends APICreateMessage implements SNSApplicationEndpointMessage, APIAuditor {
    @APIParam(maxLength = 64)
    private String phoneNumber;
    @APIParam(resourceType = SNSSmsEndpointVO.class)
    private String endpointUuid;
    @APIParam(validValues = {"AliyunSms"})
    private String type;
    @APIParam(required = false, maxLength = 255)
    private String description;

    public static APIAddSNSSmsReceiverMsg __example__() {
        APIAddSNSSmsReceiverMsg msg = new APIAddSNSSmsReceiverMsg();
        msg.setPhoneNumber("13333333333");
        msg.setEndpointUuid("467e77c4cc42492f9794429e689a9874");
        msg.setType("AliyunSms");
        msg.setDescription("description");
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getApplicationEndpointUuid() {
        return endpointUuid;
    }

    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddSNSSmsReceiverEvent) rsp).getInventory().getUuid() : "", SNSSmsReceiverVO.class);
    }
}
