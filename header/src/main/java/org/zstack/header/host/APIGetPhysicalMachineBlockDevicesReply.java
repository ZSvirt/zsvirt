package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

import static org.zstack.header.host.BlockDevicesParser.blockDevicesExample;

@RestResponse(allTo = "blockDevices")
public class APIGetPhysicalMachineBlockDevicesReply extends APIReply {
    BlockDevices blockDevices;

    public BlockDevices getBlockDevices() {
        return blockDevices;
    }

    public void setBlockDevices(BlockDevices blockDevices) {
        this.blockDevices = blockDevices;
    }

    public static APIGetPhysicalMachineBlockDevicesReply __example__() {
        APIGetPhysicalMachineBlockDevicesReply reply = new APIGetPhysicalMachineBlockDevicesReply();
        BlockDevices blockDevices = BlockDevices.valueOf(BlockDevicesParser.parse(blockDevicesExample));
        blockDevices.filter(Collections.singletonList("rom"));
        reply.setBlockDevices(blockDevices);
        return reply;
    }
}