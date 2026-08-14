package org.zstack.kvm;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.vm.VmBootDevice;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;

public class CdRomBootOrderAllocator implements BootOrderAllocator {
    private String deviceType = VmBootDevice.CdRom.toString();

    @Override
    public String getDeviceType() {
        return deviceType;
    }

    @Override
    public int allocateBootOrder(KVMAgentCommands.StartVmCmd cmd, VmInstanceSpec spec, int bootOrderNum) {
        return setCdRomBootOrder(cmd.getCdRoms(), bootOrderNum);
    }

    private int setCdRomBootOrder(List<KVMAgentCommands.CdRomTO> cdRoms, int bootOrderNum) {
        for (KVMAgentCommands.CdRomTO cdRom : cdRoms) {
            if (!cdRom.isEmpty() && StringUtils.isNotEmpty(cdRom.getPath())) {
                cdRom.setBootOrder(++bootOrderNum);
            }
        }
        return bootOrderNum;
    }
}
