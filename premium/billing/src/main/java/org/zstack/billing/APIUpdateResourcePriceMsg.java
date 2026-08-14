package org.zstack.billing;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import static org.zstack.header.message.APIParam.SCOPE_ALLOWED_SHARING;

/**
 * Created by lining on 2019/10/24.
 */
@RestRequest(
        path = "/billings/prices/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateResourcePriceEvent.class,
        isAction = true
)
public class APIUpdateResourcePriceMsg extends APIMessage {
    @APIParam(resourceType = PriceVO.class, scope = SCOPE_ALLOWED_SHARING)
    private String uuid;

    @APIParam(required = false)
    private Long endDateInLong;

    @APIParam(required = false)
    private boolean setEndDateInLongBaseOnCurrentTime;

    public static APIUpdateResourcePriceMsg __example__() {
        APIUpdateResourcePriceMsg msg = new APIUpdateResourcePriceMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getEndDateInLong() {
        return endDateInLong;
    }

    public void setEndDateInLong(Long endDateInLong) {
        this.endDateInLong = endDateInLong;
    }

    public boolean isSetEndDateInLongBaseOnCurrentTime() {
        return setEndDateInLongBaseOnCurrentTime;
    }

    public void setSetEndDateInLongBaseOnCurrentTime(boolean setEndDateInLongBaseOnCurrentTime) {
        this.setEndDateInLongBaseOnCurrentTime = setEndDateInLongBaseOnCurrentTime;
    }
}
