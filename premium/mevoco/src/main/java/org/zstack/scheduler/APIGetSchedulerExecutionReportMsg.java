package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/scheduler/report",
        method = HttpMethod.GET,
        responseClass = APIGetSchedulerExecutionReportReply.class
)
public class APIGetSchedulerExecutionReportMsg extends APISyncCallMessage {
    @APIParam
    private long startTime;

    @APIParam(validValues = {"Hour", "Day", "Month"})
    private String intervalTimeUnit;

    @APIParam(numberRange = {0, 31})
    private int range;

    @APIParam(nonempty = true)
    private List<String> schedulerJobTypes;

    public void setIntervalTimeUnit(String intervalTimeUnit) {
        this.intervalTimeUnit = intervalTimeUnit;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public void setSchedulerJobTypes(List<String> schedulerJobTypes) {
        this.schedulerJobTypes = schedulerJobTypes;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getRange() {
        return range;
    }

    public List<String> getSchedulerJobTypes() {
        return schedulerJobTypes;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getIntervalTimeUnit() {
        return intervalTimeUnit;
    }

    public static APIGetSchedulerExecutionReportMsg __example__() {
        APIGetSchedulerExecutionReportMsg msg = new APIGetSchedulerExecutionReportMsg();
        msg.startTime = 1585670400000L;
        msg.intervalTimeUnit = "Month";
        msg.range = 4;
        msg.schedulerJobTypes = Collections.singletonList(SchedulerType.VM_BACKUP);
        return msg;
    }
}
