package org.zstack.zwatch.api;

import java.util.List;
import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zwatch/alert-histories/histogram",
        method = HttpMethod.GET,
        responseClass = APIGetZWatchAlertHistogramReply.class)
public class APIGetZWatchAlertHistogramMsg extends APISyncCallMessage {

    @APIParam
    private String tableName;

    @APIParam(numberRange = {0, Long.MAX_VALUE})
    private Long startTime;

    @APIParam(numberRange = {0, Long.MAX_VALUE})
    private Long endTime;

    @APIParam(numberRange = {1, 24})
    private Integer intervalHours = 1;

    @APIParam(required = false)
    private List<String> groupColumns;

    // private List conditions;

    public static APIGetZWatchAlertHistogramMsg __example__() {
        APIGetZWatchAlertHistogramMsg ret = new APIGetZWatchAlertHistogramMsg();
        ret.startTime = DocUtils.dateInSeconds();
        ret.endTime = DocUtils.dateInSecondsAndAddSeconds(60);
        return ret;
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

    public Integer getIntervalHours() {
        return intervalHours;
    }

    public void setIntervalHours(Integer intervalHours) {
        this.intervalHours = intervalHours;
    }


    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getGroupColumns() {
        return groupColumns;
    }

    public void setGroupColumns(List<String> groupColumns) {
        this.groupColumns = groupColumns;
    }
}
