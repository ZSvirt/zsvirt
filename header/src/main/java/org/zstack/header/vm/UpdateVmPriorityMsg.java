package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 20:13 2019/9/18
 */
public class UpdateVmPriorityMsg extends NeedReplyMessage implements HostMessage {

    private List<PriorityConfigStruct> priorityConfigStructs;

    private String hostUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<PriorityConfigStruct> getPriorityConfigStructs() {
        return priorityConfigStructs;
    }

    public void setPriorityConfigStructs(List<PriorityConfigStruct> priorityConfigStructs) {
        this.priorityConfigStructs = priorityConfigStructs;
    }
}