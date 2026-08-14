package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author : jingwang
 * @create 2023/4/18 9:47 AM
 */
@RestResponse(fieldsTo = "all")
public class APIPowerOnHostEvent extends APIEvent {
    private HostInventory inventory;

    public APIPowerOnHostEvent() { super(null); }

    public APIPowerOnHostEvent(String apiId) {
        super(apiId);
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }

    public static APIPowerOnHostEvent __example__() {

        APIPowerOnHostEvent event = new APIPowerOnHostEvent();
        HostInventory hi = new HostInventory();
        hi.setName("example");
        hi.setClusterUuid(uuid());
        hi.setManagementIp("192.168.0.1");
        hi.setAvailableCpuCapacity(100000L);
        hi.setAvailableMemoryCapacity(100000L);
        hi.setIpmiAddress("192.168.0.1");
        hi.setIpmiUsername("admin");
        hi.setIpmiPort(623);
        return event;
    }
}
