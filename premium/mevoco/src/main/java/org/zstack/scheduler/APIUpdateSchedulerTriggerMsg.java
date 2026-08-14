package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerTriggerVO;

import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/8.
 */

@RestRequest(
        path = "/scheduler/triggers/{uuid}/actions",
        responseClass = APIUpdateSchedulerTriggerEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
public class APIUpdateSchedulerTriggerMsg extends APIMessage {
    @APIParam(resourceType = SchedulerTriggerVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(numberRange = {1, Integer.MAX_VALUE}, required = false)
    private Integer schedulerInterval;
    @APIParam(numberRange = {0, Integer.MAX_VALUE}, required = false)
    private Integer repeatCount;
    @APIParam(required = false)
    private Long startTime;
    @APIParam(required = false)
    private Long stopTime;
    @APIParam(required = false)
    private String cron;
    @APIParam(required = false, validValues = {"cron", "simple"})
    private String schedulerType;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
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

    public String getSchedulerType() {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    public static APIUpdateSchedulerTriggerMsg __example__() {
        APIUpdateSchedulerTriggerMsg msg = new APIUpdateSchedulerTriggerMsg();
        msg.setUuid(uuid());
        msg.setName("Test2");
        msg.setDescription("new test");
        msg.setStartTime(new Timestamp(org.zstack.header.message.DocUtils.date).getTime());
        msg.setSchedulerInterval(3600);
        msg.setRepeatCount(100);
        msg.setSchedulerType("cron");

        return msg;
    }
}
