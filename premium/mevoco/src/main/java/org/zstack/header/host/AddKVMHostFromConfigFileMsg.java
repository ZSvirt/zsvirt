package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.kvm.KVMConstant;

public class AddKVMHostFromConfigFileMsg extends NeedReplyMessage implements AddHostFromConfigFileMessage {
    private String hostInfo;

    @Override
    public String getHostInfo() {
        return hostInfo;
    }

    public void setHostInfo(String hostInfo) {
        this.hostInfo = hostInfo;
    }

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

}
