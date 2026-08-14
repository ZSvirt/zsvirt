package org.zstack.billing;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/billings/billings",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteBillingEvent.class
)
public class APIDeleteBillingMsg extends APIDeleteMessage {

    @APIParam(required = false)
    private String accountUuid;

    @APIParam(required = false)
    private Long startTime;

    @APIParam(required = false)
    private Long endTime;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public static APIDeleteBillingMsg __example__() {
        APIDeleteBillingMsg msg = new APIDeleteBillingMsg();
        return msg;
    }
}
