package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created with IntelliJ IDEA.
 * User: shixin
 * Time: 9:38 PM
 * To change this template use File | Settings | File Templates.
 */
@RestRequest(
        path = "/accounts/sessions/{sessionUuid}/renew",
        responseClass = APIRenewSessionEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APIRenewSessionMsg extends APIMessage {
    @APIParam(resourceType = SessionVO.class)
    private String sessionUuid;

    /* range from 1 mins to 365 days */
    @APIParam(required = false, numberRange = {60L, 31536000L})
    private Long duration;

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public static APIRenewSessionMsg __example__() {
        APIRenewSessionMsg msg = new APIRenewSessionMsg();
        msg.setSessionUuid(uuid());
        msg.setDuration(100L);
        return msg;
    }

}