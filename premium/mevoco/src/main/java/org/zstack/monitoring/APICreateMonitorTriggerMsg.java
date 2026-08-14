package org.zstack.monitoring;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/6/3.
 */
@RestRequest(
        path = "/monitoring/triggers",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateMonitorTriggerEvent.class
)
@Deprecated
public class APICreateMonitorTriggerMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048)
    private String expression;
    @APIParam(numberRange = {1, Integer.MAX_VALUE})
    private Integer duration;
    @APIParam(maxLength = 2048, required = false)
    private String recoveryExpression;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 32)
    private String targetResourceUuid;

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getRecoveryExpression() {
        return recoveryExpression;
    }

    public void setRecoveryExpression(String recoveryExpression) {
        this.recoveryExpression = recoveryExpression;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public static APICreateMonitorTriggerMsg __example__() {
        APICreateMonitorTriggerMsg msg = new APICreateMonitorTriggerMsg();
        msg.name = "trigger";
        msg.expression = "vm.cpu.util{} > 80";
        msg.duration = 60;
        msg.targetResourceUuid = uuid();
        return msg;
    }
}
