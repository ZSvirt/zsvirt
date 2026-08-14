package org.zstack.header.console;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/4/8.
 */
public class PingConsoleProxyAgentMsg extends NeedReplyMessage implements ConsoleProxyAgentMessage {
    private String agentUuid;

    public void setAgentUuid(String agentUuid) {
        this.agentUuid = agentUuid;
    }

    public String getAgentUuid() {
        return null;
    }
}
