package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@RestRequest(
        path = "/hostSchedulingRuleGroup/{uuid}",
        method = HttpMethod.PUT,
        responseClass = APIUpdateHostSchedulingRuleGroupEvent.class,
        isAction = true
)
public class APIUpdateHostSchedulingRuleGroupMsg extends APIMessage implements HostSchedulingRuleGroupMessage {
    @APIParam(resourceType = HostSchedulingRuleGroupVO.class)
    private String uuid;
    @APIParam(required = false)
    private String name;

    @APIParam(required = false)
    private String description;

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
    public String getHostGroupUuid() {
        return uuid;
    }

    public static APIUpdateHostSchedulingRuleGroupMsg __example__() {
        APIUpdateHostSchedulingRuleGroupMsg msg = new APIUpdateHostSchedulingRuleGroupMsg();
        msg.setUuid(uuid());
        msg.setName("test");
        msg.setDescription("desc");
        return msg;
    }
}
