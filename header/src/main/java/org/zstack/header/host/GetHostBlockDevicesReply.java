package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class GetHostBlockDevicesReply extends MessageReply {
    List<BlockDevices.BlockDevice> blockDevices;

    public List<BlockDevices.BlockDevice> getBlockDevices() {
        return blockDevices;
    }

    public void setBlockDevices(List<BlockDevices.BlockDevice> blockDevices) {
        this.blockDevices = blockDevices;
    }
}
