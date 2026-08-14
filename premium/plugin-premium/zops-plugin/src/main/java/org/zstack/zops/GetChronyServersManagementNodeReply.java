package org.zstack.zops;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class GetChronyServersManagementNodeReply extends MessageReply {
    private List<ChronyServerInfoPair> servers;

    public List<ChronyServerInfoPair> getServers() {
        return servers;
    }

    public void setServers(List<ChronyServerInfoPair> servers) {
        this.servers = servers;
    }
}
