package org.zstack.zwatch.thirdparty.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformInventory;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIUpdateThirdpartyPlatformEvent extends APIEvent {
    private ThirdpartyPlatformInventory inventory;

    public ThirdpartyPlatformInventory getInventory() {
        return inventory;
    }

    public void setInventory(ThirdpartyPlatformInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateThirdpartyPlatformEvent() {
    }

    public APIUpdateThirdpartyPlatformEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateThirdpartyPlatformEvent __example__() {
        APIUpdateThirdpartyPlatformEvent reply = new APIUpdateThirdpartyPlatformEvent();
        ThirdpartyPlatformInventory inv = new ThirdpartyPlatformInventory();

        inv.setUuid(uuid());
        inv.setName("test-load-balance-profile");
        inv.setDescription("just for test");
        //inv.setType(AutoScalingGroupConstants.SCALING_RESOURCE_TYPE_VM_INSTANCE);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventory(inv);
        return reply;
    }
}
