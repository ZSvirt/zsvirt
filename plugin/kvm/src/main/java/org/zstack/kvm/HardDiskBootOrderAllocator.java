package org.zstack.kvm;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.vm.VmBootDevice;
import org.zstack.header.vm.VmInstanceSpec;

public class HardDiskBootOrderAllocator implements BootOrderAllocator {
    private String deviceType = VmBootDevice.HardDisk.toString();

    @Override
    public String getDeviceType() {
        return deviceType;
    }

    @Override
    public int allocateBootOrder(KVMAgentCommands.StartVmCmd cmd, VmInstanceSpec spec, int bootOrderNum) {
        return setHardDiskBootOrder(cmd.getRootVolume(), bootOrderNum);
    }

    private int setHardDiskBootOrder(VolumeTO rootVolume, int bootOrderNum) {
        if (StringUtils.isNotEmpty(rootVolume.getInstallPath())) {
            rootVolume.setBootOrder(++bootOrderNum);
        }
        return bootOrderNum;
    }
}
