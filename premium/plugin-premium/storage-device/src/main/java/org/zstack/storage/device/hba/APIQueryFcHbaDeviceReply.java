package org.zstack.storage.device.hba;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/20 11:52
 */
@RestResponse(allTo = "inventories")

public class APIQueryFcHbaDeviceReply extends APIQueryReply {
    private List<HbaDeviceInventory> inventories;

    public List<HbaDeviceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HbaDeviceInventory> inventories) {
        this.inventories = inventories;
    }
    public static APIQueryFcHbaDeviceReply __example__() {
        APIQueryFcHbaDeviceReply reply = new APIQueryFcHbaDeviceReply();

        FcHbaDeviceInventory hi = new FcHbaDeviceInventory();

        hi.setUuid(uuid());
        hi.setHostUuid(uuid());
        hi.setHbaType("FC");
        hi.setPortName("10000090fab38778");
        hi.setName("host");
        hi.setSupportedClasses("Class 3");
        hi.setSupportedSpeeds("16 Gbit");
        hi.setSymbolicName("QLE2692 FW:v9.07.00 DVR:v10.02.00.106-k");
        hi.setPortState("Online");
        hi.setSpeed("8 Gbit");

        reply.setInventories(Arrays.asList(hi));
        return reply;
    }
}
