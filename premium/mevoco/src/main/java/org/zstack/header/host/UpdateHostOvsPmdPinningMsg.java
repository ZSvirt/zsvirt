package org.zstack.header.host;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by haibiao.xiao on 5/24/2022
 */
public class UpdateHostOvsPmdPinningMsg extends NeedReplyMessage implements HostMessage  {
    private String hostUuid;

    public String getPmdCpuPinning() {
        return pmdCpuPinning;
    }

    public void setPmdCpuPinning(String pmdCpuPinning) {
        this.pmdCpuPinning = pmdCpuPinning;
    }

    @NoLogging
    private String pmdCpuPinning;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
