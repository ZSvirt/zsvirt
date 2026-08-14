package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.datatype.Label;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.zwatch.ZWatchConstants.*;

/**
 *  Create by lining at 2018/11/13
 */
@RestRequest(
        path = "/zwatch/events/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateEventDataEvent.class
)
public class APIUpdateEventDataMsg extends APIMessage {
    @APIParam(required = false)
    private String dataUuid;

    @APIParam(required = false)
    private Long dataStartTime;

    @APIParam(required = false)
    private Long dataEndTime;

    @APIParam(validValues = {UPDATE_DATA_MODE_ONLYONE, UPDATE_DATA_MODE_INRANGE, UPDATE_DATA_MODE_ALL})
    private String updateMode;

    @APIParam(validValues = {DATA_READ_STATUS_READ, DATA_READ_STATUS_UNREAD}, required = false)
    private String readStatus;

    @APINoSee
    private List<Label> labelList = new ArrayList<>();

    public static APIUpdateEventDataMsg __example__() {
        APIUpdateEventDataMsg msg = new APIUpdateEventDataMsg();
        return msg;
    }

    public String getDataUuid() {
        return dataUuid;
    }

    public void setDataUuid(String dataUuid) {
        this.dataUuid = dataUuid;
    }

    public Long getDataStartTime() {
        return dataStartTime;
    }

    public void setDataStartTime(Long dataStartTime) {
        this.dataStartTime = dataStartTime;
    }

    public Long getDataEndTime() {
        return dataEndTime;
    }

    public void setDataEndTime(Long dataEndTime) {
        this.dataEndTime = dataEndTime;
    }

    public String getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(String updateMode) {
        this.updateMode = updateMode;
    }

    public String getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(String readStatus) {
        this.readStatus = readStatus;
    }

    public List<Label> getLabelList() {
        return labelList;
    }

    public void setLabelList(List<Label> labelList) {
        this.labelList = labelList;
    }
}
