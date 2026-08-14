package org.zstack.guesttools;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2019-09-18.
 */
public class DownloadGuestToolsIsoToHostMsg extends NeedReplyMessage {
    private String hostUuid;
    private String platform;
    private String hypervisorType;

    public String getHypervisorType() { return hypervisorType;}

    public void setHypervisorType(String hypervisorType) { this.hypervisorType = hypervisorType;}

    public String getPlatform() { return platform;}

    public void setPlatform(String platform) { this.platform = platform;}

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
