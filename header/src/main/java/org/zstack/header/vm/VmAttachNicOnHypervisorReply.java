package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.devices.VirtualDeviceInfo;

import java.util.List;

/**
 */
public class VmAttachNicOnHypervisorReply extends MessageReply {
    List<VirtualDeviceInfo> virtualDeviceInfoList;

    public List<VirtualDeviceInfo> getVirtualDeviceInfoList() {
        return virtualDeviceInfoList;
    }

    public void setVirtualDeviceInfoList(List<VirtualDeviceInfo> virtualDeviceInfoList) {
        this.virtualDeviceInfoList = virtualDeviceInfoList;
    }
}
