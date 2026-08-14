package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmBootDevice;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicVO;

import java.util.List;

public class NetworkBootOrderAllocator implements BootOrderAllocator {
    @Autowired
    DatabaseFacade dbf;

    private String deviceType = VmBootDevice.Network.toString();

    @Override
    public String getDeviceType() {
        return deviceType;
    }

    @Override
    public int allocateBootOrder(KVMAgentCommands.StartVmCmd cmd, VmInstanceSpec spec, int bootOrderNum) {
        return setNetWorkBootOrder(cmd.getNics(), spec, bootOrderNum);
    }

    private int setNetWorkBootOrder(List<KVMAgentCommands.NicTO> nics, VmInstanceSpec spec, int bootOrderNum) {
        if (nics.size() == 0) {
            return bootOrderNum;
        }

        int defaultOrderBootNum = ++bootOrderNum;
        boolean isDefaultL3Network = false;

        for (KVMAgentCommands.NicTO nic : nics) {
            VmNicVO nicVO = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
            if (!isDefaultL3Network && nicVO != null & nicVO.getL3NetworkUuid().equals(spec.getVmInventory().getDefaultL3NetworkUuid())) {
                nic.setBootOrder(defaultOrderBootNum);
                isDefaultL3Network = true;
            } else {
                nic.setBootOrder(++bootOrderNum);
            }
        }
        return bootOrderNum;
    }
}
