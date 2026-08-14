package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class SetBridgeRouterPortMsg  extends NeedReplyMessage implements HostMessage{
    private List<String> nicNames;
    private String hostUuid;
    private Boolean enable;

    public List<String> getNicNames() {
        return nicNames;
    }

    public void setNicNames(List<String> nicNames) {
        this.nicNames = nicNames;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }
}
