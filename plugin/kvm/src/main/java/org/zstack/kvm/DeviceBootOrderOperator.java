package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.VmBootDevice;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DeviceBootOrderOperator implements Component {
    @Autowired
    private PluginRegistry pluginRgty;

    private Map<String, BootOrderAllocator> allocators = Collections.synchronizedMap(new HashMap<>());

    public void updateVmDeviceBootOrder(KVMAgentCommands.StartVmCmd cmd, VmInstanceSpec spec) {
        int bootOrderNum = 0;
        BootOrderAllocator allocator;
        for (String bootOrder : spec.getBootOrders()) {
            if (VmBootDevice.HardDisk.toString().equals(bootOrder)) {
                allocator = getBootOrderAllocator(VmBootDevice.HardDisk.toString());
                bootOrderNum = allocator.allocateBootOrder(cmd, spec, bootOrderNum);
            } else if (VmBootDevice.CdRom.toString().equals(bootOrder)) {
                allocator = getBootOrderAllocator(VmBootDevice.CdRom.toString());
                bootOrderNum = allocator.allocateBootOrder(cmd, spec, bootOrderNum);
            } else if (VmBootDevice.Network.toString().equals(bootOrder)) {
                allocator = getBootOrderAllocator(VmBootDevice.Network.toString());
                bootOrderNum = allocator.allocateBootOrder(cmd, spec, bootOrderNum);
            } else {
                throw new CloudRuntimeException(String.format("unknown boot device[%s]", bootOrder));
            }
        }
    }

    public BootOrderAllocator getBootOrderAllocator(String deviceType) {
        BootOrderAllocator allocator = allocators.get(deviceType);
        if (allocator == null) {
            throw new CloudRuntimeException(String.format("No BootOrderAllocator for device type: %s found", deviceType));
        }
        return allocator;
    }

    @Override
    public boolean start() {
        for (BootOrderAllocator allocator : pluginRgty.getExtensionList(BootOrderAllocator.class)) {
            BootOrderAllocator old = allocators.get(allocator.getDeviceType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate BootOrderAllocator[%s, %s] for the device type[%s]",
                        allocator.getClass().getName(), old.getClass().getName(), old.getDeviceType()));
            }
            allocators.put(allocator.getDeviceType(), allocator);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
