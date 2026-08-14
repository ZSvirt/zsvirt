package org.zstack.pciDevice.gpu;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/8 18:31
 */
@RestResponse(allTo = "inventories")
public class APIQueryGpuDeviceReply extends APIQueryReply {
    private List<GpuDeviceInventory> inventories;

    public List<GpuDeviceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<GpuDeviceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryGpuDeviceReply __example__() {
        APIQueryGpuDeviceReply reply = new APIQueryGpuDeviceReply();
        GpuDeviceInventory inv = new GpuDeviceInventory();

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
