package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2022/11/30
 */
@RestRequest(
        path = "/vmSchedulingRule/{uuid}/update",
        method = HttpMethod.PUT,
        responseClass = APIUpdateVmSchedulingRuleEvent.class,
        isAction = true
)
public class APIUpdateVmSchedulingRuleMsg extends APIMessage implements VmSchedulingRuleMessage  {
    @APIParam(resourceType = VmSchedulingRuleVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(required = false)
    private String mode;

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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String getVmSchedulingRuleUuid() {
        return uuid;
    }

    public static APIUpdateVmSchedulingRuleMsg __example__() {
        APIUpdateVmSchedulingRuleMsg msg = new APIUpdateVmSchedulingRuleMsg();
        msg.setName("new name");
        msg.setDescription("desc");
        msg.setUuid(uuid());
        msg.setMode("SOFT");

        return msg;
    }
}
