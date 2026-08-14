package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;


/**
 * @apiResult api event for '*AddHost*' message, depending on hypervisor the messages vary
 * <p>
 * for example(:ref:`APIAddKVMHostMsg`)
 * @example {
 * "org.zstack.header.host.APIAddHostEvent": {
 * "inventory": {
 * "zoneUuid": "2893ce85c43d4a3a8d78f414da39966e",
 * "name": "host1-192.168.0.203",
 * "uuid": "43673938584447b2a29ab3d53f9d88d3",
 * "clusterUuid": "8524072a4274403892bcc5b1972c2576",
 * "description": "Test",
 * "managementIp": "192.168.0.203",
 * "hypervisorType": "KVM",
 * "state": "Enabled",
 * "status": "Connected",
 * "createDate": "Feb 28, 2014 6:49:24 PM",
 * "lastOpDate": "Feb 28, 2014 6:49:24 PM"
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIAddHostEvent extends APIEvent {
    /**
     * @desc see :ref:`HostInventory`
     */
    private HostInventory inventory;

    public APIAddHostEvent() {
        super(null);
    }

    public APIAddHostEvent(String apiId) {
        super(apiId);
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddHostEvent __example__() {
        APIAddHostEvent event = new APIAddHostEvent();
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
