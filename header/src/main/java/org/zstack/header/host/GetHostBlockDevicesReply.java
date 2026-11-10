package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;
import java.util.Map;

public class GetHostBlockDevicesReply extends MessageReply {
    List<BlockDevices.BlockDevice> blockDevices;
    private Map<String, String> blockDeviceWwids;
    private Map<String, String> blockDeviceWwns;

    public Map<String, String> getBlockDeviceWwids() {
        return blockDeviceWwids;
    }

    public void setBlockDeviceWwids(Map<String, String> blockDeviceWwids) {
        this.blockDeviceWwids = blockDeviceWwids;
    }

    public Map<String, String> getBlockDeviceWwns() {
        return blockDeviceWwns;
    }

    public void setBlockDeviceWwns(Map<String, String> blockDeviceWwns) {
        this.blockDeviceWwns = blockDeviceWwns;
    }

    public List<BlockDevices.BlockDevice> getBlockDevices() {
        return blockDevices;
    }

    public void setBlockDevices(List<BlockDevices.BlockDevice> blockDevices) {
        this.blockDevices = blockDevices;
    }
}
