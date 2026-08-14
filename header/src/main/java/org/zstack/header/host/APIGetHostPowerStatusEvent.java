package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author : jingwang
 * @create 2023/4/18 9:51 AM
 */
@RestResponse(fieldsTo = "all")
public class APIGetHostPowerStatusEvent extends APIEvent {
    HostIpmiInventory inventory;

    public APIGetHostPowerStatusEvent() {
    }

    public APIGetHostPowerStatusEvent(String apiId) {
        super(apiId);
    }

    public HostIpmiInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostIpmiInventory inventory) {
        this.inventory = inventory;
    }

    public static APIGetHostPowerStatusEvent __example__() {
        APIGetHostPowerStatusEvent event = new APIGetHostPowerStatusEvent();
        HostIpmiInventory hi = new HostIpmiInventory ();
        hi.setIpmiAddress("192.168.0.1");
        hi.setIpmiPort(623);
        hi.setIpmiUsername("admin");
        hi.setIpmiPassword("password");
        hi.setIpmiPowerStatus(HostPowerStatus.POWER_ON.toString());
        event.setInventory(hi);
        return event;
    }
}
