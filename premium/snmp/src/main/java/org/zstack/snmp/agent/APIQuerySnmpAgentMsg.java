package org.zstack.snmp.agent;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 *
 * @Author : jingwang
 * @create 2023/8/1 10:29
 */
@AutoQuery(replyClass = APIQuerySnmpAgentReply.class, inventoryClass = SnmpAgentInventory.class)
@RestRequest(
        path = "/snmp/agent",
        optionalPaths = {"/snmp/agent/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySnmpAgentReply.class
)
public class APIQuerySnmpAgentMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
