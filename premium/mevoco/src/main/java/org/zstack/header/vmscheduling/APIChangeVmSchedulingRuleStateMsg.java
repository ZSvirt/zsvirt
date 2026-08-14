package org.zstack.header.vmscheduling;


import org.springframework.http.HttpMethod;
import org.zstack.header.affinitygroup.AffinityGroupStateEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2022/11/30
 */
@RestRequest(
        path = "/vmSchedulingRule/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeVmSchedulingRuleStateEvent.class,
        isAction = true
)
public class APIChangeVmSchedulingRuleStateMsg extends APIMessage implements VmSchedulingRuleMessage {
    @APIParam(resourceType = VmSchedulingRuleVO.class)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String state;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public static APIChangeVmSchedulingRuleStateMsg __example__() {
        APIChangeVmSchedulingRuleStateMsg msg = new APIChangeVmSchedulingRuleStateMsg();
        msg.setUuid(uuid());
        msg.setState(AffinityGroupStateEvent.enable.toString());

        return msg;
    }

    @Override
    public String getVmSchedulingRuleUuid() {
        return uuid;
    }
}
