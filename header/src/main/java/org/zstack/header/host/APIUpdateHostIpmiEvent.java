package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * @Author : jingwang
 * @create 2023/4/18 11:28 AM
 */
@RestResponse(fieldsTo = {"all"})
public class APIUpdateHostIpmiEvent extends APIEvent {
    private HostIpmiInventory hostIpmiInventory;

    public APIUpdateHostIpmiEvent() { super(null); }

    public APIUpdateHostIpmiEvent(String apiId) {
        super(apiId);
    }

    public HostIpmiInventory getHostIpmiInventory() {
        return hostIpmiInventory;
    }

    public void setHostIpmiInventory(HostIpmiInventory hostIpmiInventory) {
        this.hostIpmiInventory = hostIpmiInventory;
    }

    public static APIUpdateHostIpmiEvent __example__() {
        APIUpdateHostIpmiEvent event = new APIUpdateHostIpmiEvent();
        HostIpmiInventory ipmiInventory = new HostIpmiInventory();
        ipmiInventory.setIpmiAddress("192.168.0.1");
        ipmiInventory.setIpmiUsername("admin");
        ipmiInventory.setIpmiPort(623);
        ipmiInventory.setIpmiPassword("password");
        event.setHostIpmiInventory(ipmiInventory);
        return event;
    }
}
