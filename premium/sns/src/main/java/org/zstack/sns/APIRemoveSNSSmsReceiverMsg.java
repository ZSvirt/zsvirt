package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by Qi Le on 2019-07-11
 */
@RestRequest(path = "/sns/sms-endpoints/{endpointUuid}/receivers/{phoneNumber}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveSNSSmsReceiverEvent.class)
public class APIRemoveSNSSmsReceiverMsg extends APIDeleteMessage implements SNSApplicationEndpointMessage {
//    @APIParam(resourceType = SNSSmsReceiverVO.class, successIfResourceNotExisting = true)
//    private String uuid;
    @APIParam(resourceType = SNSSmsEndpointVO.class, successIfResourceNotExisting = true)
    private String endpointUuid;
    @APIParam
    private String phoneNumber;

    public static APIRemoveSNSSmsReceiverMsg __example__() {
        APIRemoveSNSSmsReceiverMsg msg = new APIRemoveSNSSmsReceiverMsg();
        msg.endpointUuid = uuid();
        msg.phoneNumber = "18812345678";
        return msg;
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

//    public String getUuid() {
//        return uuid;
//    }
//
//    public void setUuid(String uuid) {
//        this.uuid = uuid;
//    }

    @Override
    public String getApplicationEndpointUuid() {
        return endpointUuid;
    }
}
