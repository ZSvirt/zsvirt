package org.zstack.header.vipQos;

import org.zstack.core.Platform;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by liangbo.zhou on 17-6-10.
 */
@RestResponse(allTo = "inventories")
public class APIGetVipQosReply extends APIReply {
    private List<VipQosInventory> inventories;

    public List<VipQosInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VipQosInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVipQosReply __example__() {
        APIGetVipQosReply reply = new APIGetVipQosReply();

        VipQosInventory inv = new VipQosInventory();
        inv.setUuid(uuid());
        inv.setVipUuid(uuid());
        inv.setPort(80);
        inv.setInboundBandwidth(new Long(1024 * 1024L));
        inv.setOutboundBandwidth(new Long(1024 * 1024L));

        reply.setInventories(asList(inv));

        return reply;
    }
}
