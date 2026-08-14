package org.zstack.header.console;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/3/15.
 */
@RestRequest(
        path = "/consoles/agents",
        isAction = true,
        responseClass = APIReconnectConsoleProxyAgentEvent.class,
        method = HttpMethod.PUT
)
public class APIReconnectConsoleProxyAgentMsg extends APIMessage implements ConsoleProxyAgentMessage {
    @APIParam(required = false, nonempty = true)
    private List<String> agentUuids;

    public List<String> getAgentUuids() {
        return agentUuids;
    }

    public void setAgentUuids(List<String> agentUuids) {
        this.agentUuids = agentUuids;
    }
 
    public static APIReconnectConsoleProxyAgentMsg __example__() {
        APIReconnectConsoleProxyAgentMsg msg = new APIReconnectConsoleProxyAgentMsg();
        msg.setAgentUuids(list(uuid(), uuid(), uuid()));

        return msg;
    }
}
