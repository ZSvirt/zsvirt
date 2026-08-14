package org.zstack.header.vdpa;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.kvm.KVMAgentCommands;

import java.util.List;

public class GenerateVdpaMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private List<KVMAgentCommands.NicTO> nics;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public List<KVMAgentCommands.NicTO> getNics() {
        return nics;
    }

    public void setNics(List<KVMAgentCommands.NicTO> nics) {
        this.nics = nics;
    }
}
