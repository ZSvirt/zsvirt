package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobVO;

import java.util.Map;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
@RestRequest(
        path = "/scheduler/jobs/{uuid}/actions",
        responseClass = APIUpdateSchedulerJobEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
public class APIUpdateSchedulerJobMsg extends APIMessage implements SchedulerMessage {
    @APIParam(resourceType = SchedulerJobVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
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

    @Override
    public String getSchedulerUuid() {
        return uuid;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public static APIUpdateSchedulerJobMsg __example__() {
        APIUpdateSchedulerJobMsg msg = new APIUpdateSchedulerJobMsg();
        msg.setUuid(uuid());
        msg.setName("Test2");
        msg.setDescription("new test");
        return msg;
    }
}
