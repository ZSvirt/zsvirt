package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.ZWatchConstants;

/**
 * Create by yaoning at 2020/10/20
 */
@RestRequest(
        path = "/zwatch/event-histories/acknowledgments",
        method = HttpMethod.POST,
        responseClass = APIAckAlertDataEvent.class,
        parameterName = "params"
)
public class APIAckEventDataMsg extends APIAckAlertDataMsg {
    @APIParam(maxLength = 32)
    private String eventSubscriptionUuid;

    public static APIAckEventDataMsg __example__() {
        APIAckEventDataMsg msg = new APIAckEventDataMsg();
        msg.setEventSubscriptionUuid(uuid());
        msg.setAlertDataUuid(uuid());
        msg.setDataType(ZWatchConstants.EVENT_DATA_TYPE);
        msg.setResourceUuid(uuid());
        msg.setAckPeriodSec(6000);
        return msg;
    }

    public String getEventSubscriptionUuid() {
        return eventSubscriptionUuid;
    }

    public void setEventSubscriptionUuid(String eventSubscriptionUuid) {
        this.eventSubscriptionUuid = eventSubscriptionUuid;
    }
}
