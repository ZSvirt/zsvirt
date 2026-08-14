package org.zstack.sdk;

import org.zstack.sdk.BlockDevices;

public class GetPhysicalMachineBlockDevicesResult {
    public BlockDevices blockDevices;
    public void setBlockDevices(BlockDevices blockDevices) {
        this.blockDevices = blockDevices;
    }
    public BlockDevices getBlockDevices() {
        return this.blockDevices;
    }

}
