package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointVO;
import org.zstack.zwatch.datatype.Label;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/zwatch/events",
        method = HttpMethod.GET,
        responseClass = APIGetEventDataReply.class
)
public class APIGetEventDataMsg extends APISyncCallMessage {
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long startTime;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long endTime;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long offsetAheadOfCurrentTime;
    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer limit = 100;
    private List<String> conditions;

    @APIParam(required = false)
    private boolean count;

    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer start = 0;

    @APIParam(required = false, maxLength = 256)
    private String conditionExpression;

    @APINoSee
    private List<Label> labelList;

    @APIParam(resourceType = SNSApplicationEndpointVO.class, required = false)
    private String endpointUuid;


    public static APIGetEventDataMsg __example__() {
        APIGetEventDataMsg msg = new APIGetEventDataMsg();
        msg.setStartTime(DocUtils.date);
        msg.setLimit(10);
        msg.setConditions(asList(String.format("HostUuid=%s", uuid(HostVO.class))));
        return msg;
    }

    public Long getOffsetAheadOfCurrentTime() {
        return offsetAheadOfCurrentTime;
    }

    public void setOffsetAheadOfCurrentTime(Long offsetAheadOfCurrentTime) {
        this.offsetAheadOfCurrentTime = offsetAheadOfCurrentTime;
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

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

    public List<Label> getLabelList() {
        return labelList;
    }

    public void setLabelList(List<Label> labelList) {
        this.labelList = labelList;
    }

    public boolean isCount() {
        return count;
    }

    public void setCount(boolean count) {
        this.count = count;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }


    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }
}
