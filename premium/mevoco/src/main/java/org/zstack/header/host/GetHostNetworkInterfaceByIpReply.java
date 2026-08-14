package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class GetHostNetworkInterfaceByIpReply extends MessageReply {
    private List<String> interfaceNames;

    public List<String> getInterfaceNames() {
        return interfaceNames;
    }

    public void setInterfaceNames(List<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
    }
}
