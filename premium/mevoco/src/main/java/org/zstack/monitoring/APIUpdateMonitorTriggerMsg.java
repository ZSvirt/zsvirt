package org.zstack.monitoring;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/6/10.
 */
@RestRequest(path = "/monitoring/triggers/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateMonitorTriggerEvent.class,
        isAction = true
)
@Deprecated
public class APIUpdateMonitorTriggerMsg extends APIMessage implements MonitorTriggerMessage {
    @APIParam(resourceType = MonitorTriggerVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 2048, required = false)
    private String expression;
    @APIParam(numberRange = {1, Integer.MAX_VALUE}, required = false)
    private Integer duration;

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

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    @Override
    public String getMonitorTriggerUuid() {
        return uuid;
    }

    public static APIUpdateMonitorTriggerMsg __example__() {
        APIUpdateMonitorTriggerMsg msg = new APIUpdateMonitorTriggerMsg();
        msg.uuid = uuid();
        msg.name = "trigger2";
        msg.expression = "vm.mem.util{} > 0.9";
        return msg;
    }
}
