package org.zstack.zops;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class CheckMultiHostsNetworkReachableManagementNodeReply extends MessageReply {
    List<NetworkReachablePair> result;

    public List<NetworkReachablePair> getResult() {
        return result;
    }

    public void setResult(List<NetworkReachablePair> result) {
        this.result = result;
    }
}
