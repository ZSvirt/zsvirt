package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class GetHostPhysicalCpuFactsMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private boolean noStatusCheck;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public boolean isNoStatusCheck() {
        return noStatusCheck;
    }

    public void setNoStatusCheck(boolean noStatusCheck) {
        this.noStatusCheck = noStatusCheck;
    }
}
