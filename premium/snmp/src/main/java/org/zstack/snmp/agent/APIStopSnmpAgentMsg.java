package org.zstack.snmp.agent;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.snmp.SnmpAgentInnerMessage;
import org.zstack.snmp.SnmpAgentLocalMessageBuilder;

/**
 * @Author : jingwang
 * @create 2023/7/13 10:03 AM
 */
@RestRequest(
        path = "/snmp/agent/actions",
        method = HttpMethod.PUT,
        responseClass = APIStopSnmpAgentEvent.class,
        isAction = true
)
public class APIStopSnmpAgentMsg extends APIMessage implements APIAuditor, SnmpAgentLocalMessageBuilder {
    @APIParam(resourceType = SnmpAgentVO.class)
    private String uuid;

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIStopSnmpAgentEvent)rsp).getInventory().getUuid() : "", SnmpAgentVO.class);
    }

    public static APIStopSnmpAgentMsg __example__() {
        APIStopSnmpAgentMsg msg = new APIStopSnmpAgentMsg();
        msg.setUuid(uuid(SnmpAgentVO.class));
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public SnmpAgentInnerMessage buildLocalMessage() {
        StopSnmpAgentMsg msg = new StopSnmpAgentMsg();
        msg.setUuid(getUuid());
        return msg;
    }
}
