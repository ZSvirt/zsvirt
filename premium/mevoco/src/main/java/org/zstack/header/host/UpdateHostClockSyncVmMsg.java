package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.Map;

public class UpdateHostClockSyncVmMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private Map<String, Integer> vmIntervalMap;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public Map<String, Integer> getVmIntervalMap() {
        return vmIntervalMap;
    }

    public void setVmIntervalMap(Map<String, Integer> vmIntervalMap) {
        this.vmIntervalMap = vmIntervalMap;
    }
}
