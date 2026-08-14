package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * @author: zhanyong.miao
 * @date: 2019-04-25
 **/
public class PrepareL2NetworkOnHostsMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String l2NetworkUuid;
    private List<String> hosts;

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }
}