package org.zstack.kvm.hypervisor;

import static org.zstack.kvm.KVMAgentCommands.GetVirtualizerInfoRsp;
import static org.zstack.kvm.KVMAgentCommands.VirtualizerInfoTO;

public interface KvmHypervisorInfoManager {
    void save(GetVirtualizerInfoRsp rsp);
    void saveHostInfo(VirtualizerInfoTO info);
    void saveVmInfo(VirtualizerInfoTO info);

    void clean(String uuid);

    void refreshMetadata();
}
