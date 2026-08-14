package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointVO;
import org.zstack.zwatch.datatype.Label;

import java.util.ArrayList;
import java.util.List;

@RestRequest(
        path = "/zwatch/alarm-histories", method = HttpMethod.GET, responseClass = APIGetAlarmDataReply.class)
public class APIGetAlarmDataMsg extends APISyncCallMessage {
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long startTime;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long endTime;
    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer limit = 100;
    @APIParam(required = false)
    private List<String> conditions;

    @APIParam(required = false)
    private boolean count;

    @APIParam(required = false)
    private boolean excludeOtherAccount;

    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer start = 0;

    @APIParam(resourceType = SNSApplicationEndpointVO.class, required = false)
    private String endpointUuid;


    public static APIGetAlarmDataMsg __example__() {
        APIGetAlarmDataMsg ret = new APIGetAlarmDataMsg();
        ret.startTime = DocUtils.dateInSeconds();
        ret.endTime = DocUtils.dateInSecondsAndAddSeconds(60);
        ret.limit = 200;
        return ret;
    }


    @APINoSee
    private List<Label> labelList = new ArrayList<>();

    public List<Label> getLabelList() {
        return labelList;
    }

    public void setLabelList(List<Label> labelList) {
        this.labelList = labelList;
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

    public boolean isCount() {
        return count;
    }

    public void setCount(boolean count) {
        this.count = count;
    }

    public boolean isExcludeOtherAccount() {
        return excludeOtherAccount;
    }

    public void setExcludeOtherAccount(boolean excludeOtherAccount) {
        this.excludeOtherAccount = excludeOtherAccount;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }
}
