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
 *
 * @Author : jingwang
 * @create 2023/7/13 9:58 AM
 */
@RestRequest(
        path = "/snmp/agent/actions",
        method = HttpMethod.PUT,
        responseClass = APIStartSnmpAgentEvent.class,
        isAction = true
)
public class APIStartSnmpAgentMsg extends APIMessage implements APIAuditor, SnmpAgentLocalMessageBuilder {
    @APIParam(resourceType = SnmpAgentVO.class)
    private String uuid;

    public static APIStartSnmpAgentMsg __example__() {
        APIStartSnmpAgentMsg msg = new APIStartSnmpAgentMsg();
        msg.setUuid(uuid(SnmpAgentVO.class));
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIStartSnmpAgentEvent)rsp).getInventory().getUuid() : "", SnmpAgentVO.class);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public SnmpAgentInnerMessage buildLocalMessage() {
        StartSnmpAgentMsg smsg = new StartSnmpAgentMsg();
        smsg.setUuid(getUuid());
        return smsg;
    }
}
