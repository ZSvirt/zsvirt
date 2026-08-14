package org.zstack.zwatch.thirdparty.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyOriginalAlertVO;

@RestRequest(
        path = "/zwatch/third-party/alerts/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateThirdpartyAlertsEvent.class,
        isAction = true
)
public class APIUpdateThirdpartyAlertsMsg extends APIMessage {

    @APIParam(resourceType = ThirdpartyOriginalAlertVO.class, required = false)
    private String uuid;

    @APIParam(required = false)
    private Long startTimeMillis;

    @APIParam(required = false)
    private Long endTimeMillis;

    @APIParam(validValues = {ZWatchConstants.DATA_READ_STATUS_UNREAD, ZWatchConstants.DATA_READ_STATUS_READ}, required = false)
    private String updateReadStatus;

    public static APIUpdateThirdpartyAlertsMsg __example__() {
        APIUpdateThirdpartyAlertsMsg msg = new APIUpdateThirdpartyAlertsMsg();

        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(Long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public Long getEndTimeMillis() {
        return endTimeMillis;
    }

    public void setEndTimeMillis(Long endTimeMillis) {
        this.endTimeMillis = endTimeMillis;
    }

    public String getUpdateReadStatus() {
        return updateReadStatus;
    }

    public void setUpdateReadStatus(String updateReadStatus) {
        this.updateReadStatus = updateReadStatus;
    }
}
