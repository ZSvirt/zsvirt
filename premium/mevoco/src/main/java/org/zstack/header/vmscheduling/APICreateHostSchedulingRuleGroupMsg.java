package org.zstack.header.vmscheduling;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@RestRequest(
        path = "/hostSchedulingRuleGroup",
        method = HttpMethod.POST,
        responseClass = APICreateHostSchedulingRuleGroupEvent.class,
        parameterName = "params"
)
public class APICreateHostSchedulingRuleGroupMsg extends APICreateMessage implements APIAuditor {
    @APIParam
    private String name;
    @APIParam(required = false)
    private String description;
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }


    public static APICreateHostSchedulingRuleGroupMsg __example__() {
        APICreateHostSchedulingRuleGroupMsg msg = new APICreateHostSchedulingRuleGroupMsg();
        msg.setZoneUuid(uuid());
        msg.setName("test");
        msg.setDescription("test desc");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APICreateHostSchedulingRuleGroupEvent evt = (APICreateHostSchedulingRuleGroupEvent) rsp;
            uuid = evt.getInventory().getUuid();
        }
        return new Result(uuid, HostSchedulingRuleGroupVO.class);
    }
}
