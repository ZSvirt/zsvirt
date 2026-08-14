package org.zstack.header.vipQos;

import org.zstack.core.Platform;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by liangbo.zhou on 17-6-10.
 */
@RestResponse(allTo = "inventory")
public class APISetVipQosEvent extends APIEvent{
    private VipQosInventory inventory;

    public VipQosInventory getInventory() {
        return inventory;
    }

    public void setInventory(VipQosInventory inventory) {
        this.inventory = inventory;
    }

    public APISetVipQosEvent(){
        super();
    }

    public APISetVipQosEvent(String apiId){
        super(apiId);
    }

    public static APISetVipQosEvent __example__(){
        APISetVipQosEvent event = new APISetVipQosEvent();

        VipQosInventory inv = new VipQosInventory();
        inv.setUuid(uuid());
        inv.setVipUuid(uuid());
        inv.setPort(80);
        inv.setInboundBandwidth(new Long(1024 * 1024L));
        inv.setOutboundBandwidth(new Long(1024 * 1024L));

        event.setInventory(inv);

        return  event;
    }
}
