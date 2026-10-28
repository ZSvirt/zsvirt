package org.zstack.header.console;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/3/15.
 */
@AutoQuery(replyClass = APIQueryConsoleProxyAgentReply.class, inventoryClass = ConsoleProxyAgentInventory.class)
@RestRequest(
        path = "/consoles/agents",
        optionalPaths = "/consoles/agents/{uuid}",
        method = HttpMethod.GET,
        responseClass = APIQueryConsoleProxyAgentReply.class
)
public class APIQueryConsoleProxyAgentMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return list("uuid=" + uuid(ConsoleProxyAgentVO.class));
    }
}
