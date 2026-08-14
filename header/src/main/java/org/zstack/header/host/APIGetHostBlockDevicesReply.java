package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zstack.header.host.BlockDevicesParser.blockDevicesExample;

@RestResponse(allTo = "blockDevices")
public class APIGetHostBlockDevicesReply extends APIReply {
    List<BlockDevices.BlockDevice> blockDevices;

    public List<BlockDevices.BlockDevice> getBlockDevices() {
        return blockDevices;
    }

    public void setBlockDevices(List<BlockDevices.BlockDevice> blockDevices) {
        this.blockDevices = blockDevices;
    }

    public static APIGetHostBlockDevicesReply __example__() {
        APIGetHostBlockDevicesReply reply = new APIGetHostBlockDevicesReply();
        return reply;
    }
}