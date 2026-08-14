package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerTriggerVO;

import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */

@RestRequest(
        path = "/scheduler/triggers",
        method = HttpMethod.POST,
        responseClass = APICreateSchedulerTriggerEvent.class,
        parameterName = "params"
)
public class APICreateSchedulerTriggerMsg extends APICreateMessage implements APIAuditor {
    @APIParam(required = true, maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(numberRange = {1, Integer.MAX_VALUE}, required = false)
    private Integer schedulerInterval;

    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer repeatCount = 0;

    @APIParam(required = false)
    private Long startTime;

    @APIParam(required = false)
    private Long stopTime;

    @APIParam(required = true, validValues = {"simple", "cron"})
    private String schedulerType;

    @APIParam(required = false)
    private String cron;

    public String getSchedulerType() {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(Integer schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getStopTime() {
        return stopTime;
    }

    public void setStopTime(Long stopTime) {
        this.stopTime = stopTime;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public static APICreateSchedulerTriggerMsg __example__ () {
        APICreateSchedulerTriggerMsg msg = new APICreateSchedulerTriggerMsg();
        msg.setName("trigger");
        msg.setDescription("description");
        msg.setStartTime(new Timestamp(org.zstack.header.message.DocUtils.date).getTime());
        msg.setSchedulerType("simple");
        msg.setSchedulerInterval(3600);
        msg.setRepeatCount(100);

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateSchedulerTriggerEvent)rsp).getInventory().getUuid() : "", SchedulerTriggerVO.class);
    }
}
