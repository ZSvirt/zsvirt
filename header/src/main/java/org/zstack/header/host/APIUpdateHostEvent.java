package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateHostEvent extends APIEvent {
    private HostInventory inventory;

    public APIUpdateHostEvent() {
    }

    public APIUpdateHostEvent(String apiId) {
        super(apiId);
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateHostEvent __example__() {
        APIUpdateHostEvent event = new APIUpdateHostEvent();
        HostInventory hi = new HostInventory ();
        hi.setAvailableCpuCapacity(2L);
        hi.setAvailableMemoryCapacity(4L);
        hi.setClusterUuid(uuid());
        hi.setManagementIp("192.168.0.1");
        hi.setName("example");
        hi.setState(HostState.Enabled.toString());
        hi.setStatus(HostStatus.Connected.toString());
        hi.setClusterUuid(uuid());
        hi.setZoneUuid(uuid());
        hi.setUuid(uuid());
        hi.setTotalCpuCapacity(4L);
        hi.setTotalMemoryCapacity(4L);
        hi.setHypervisorType("KVM");
        hi.setDescription("example");
        event.setInventory(hi);
        return event;
    }

}
