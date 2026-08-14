package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobGroupVO;

import java.util.Map;

@RestRequest(
        path = "/scheduler/jobgroups/{uuid}/actions",
        responseClass = APIUpdateSchedulerJobGroupEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
public class APIUpdateSchedulerJobGroupMsg extends APIMessage implements SchedulerJobGroupMessage {
    @APIParam(resourceType = SchedulerJobGroupVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(required = false, validValues = {"Enabled", "Disabled"})
    private String state;
    @APIParam(required = false)
    private Map<String, String> parameters;

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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public static APIUpdateSchedulerJobGroupMsg __example__() {
        APIUpdateSchedulerJobGroupMsg msg = new APIUpdateSchedulerJobGroupMsg();
        msg.setUuid(uuid());
        msg.setName("Test2");
        msg.setDescription("new test");
        return msg;
    }

    @Override
    public String getSchedulerJobGroupUuid() {
        return uuid;
    }
}
