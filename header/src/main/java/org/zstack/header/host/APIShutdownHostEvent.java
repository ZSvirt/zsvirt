package org.zstack.header.host;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author : jingwang
 * @create 2023/4/14 5:51 PM
 */
@RestResponse(fieldsTo = "all")
public class APIShutdownHostEvent extends APIEvent {
    private HostInventory inventory;

    public APIShutdownHostEvent() { super(null); }

    public APIShutdownHostEvent(String apiId) {
        super(apiId);
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }

    public static APIShutdownHostEvent __example__() {

        APIShutdownHostEvent event = new APIShutdownHostEvent();
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
