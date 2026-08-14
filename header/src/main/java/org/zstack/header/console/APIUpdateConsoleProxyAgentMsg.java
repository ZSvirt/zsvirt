package org.zstack.header.console;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/consoles/agents/{uuid}/actions",
        isAction = true,
        responseClass = APIUpdateConsoleProxyAgentEvent.class,
        method = HttpMethod.PUT
)
public class APIUpdateConsoleProxyAgentMsg extends APIMessage implements ConsoleProxyAgentMessage {
    @APIParam(resourceType = ConsoleProxyAgentVO.class)
    private String uuid;
    @APIParam
    private String consoleProxyOverriddenIp;
    @APIParam(required = false, numberRange={1, 65535})
    private Integer consoleProxyPort;

    public Integer getConsoleProxyPort() {
        return consoleProxyPort;
    }

    public void setConsoleProxyPort(Integer consoleProxyPort) {
        this.consoleProxyPort = consoleProxyPort;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getConsoleProxyOverriddenIp() {
        return consoleProxyOverriddenIp;
    }

    public void setConsoleProxyOverriddenIp(String consoleProxyOverriddenIp) {
        this.consoleProxyOverriddenIp = consoleProxyOverriddenIp;
    }

    public static APIUpdateConsoleProxyAgentMsg __example__() {
        APIUpdateConsoleProxyAgentMsg msg = new APIUpdateConsoleProxyAgentMsg();
        msg.setUuid(uuid());
        msg.setConsoleProxyOverriddenIp("127.0.0.1");
        msg.setConsoleProxyPort(4789);
        return msg;
    }
}
