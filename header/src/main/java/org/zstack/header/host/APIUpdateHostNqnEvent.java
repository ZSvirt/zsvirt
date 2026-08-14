package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIUpdateHostNqnEvent extends APIEvent {
    private HostInventory inventory;

    public APIUpdateHostNqnEvent() { super(null); }

    public APIUpdateHostNqnEvent(String apiId) {
        super(apiId);
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateHostNqnEvent __example__() {
        APIUpdateHostNqnEvent event = new APIUpdateHostNqnEvent();
        HostInventory host = new HostInventory();
        host.setAvailableCpuCapacity(2L);
        host.setAvailableMemoryCapacity(4L);
        host.setManagementIp("192.168.0.1");
        host.setName("example");
        host.setState(HostState.Enabled.toString());
        host.setStatus(HostStatus.Connected.toString());
        host.setClusterUuid(uuid());
        host.setZoneUuid(uuid());
        host.setUuid(uuid());
        host.setTotalCpuCapacity(4L);
        host.setTotalMemoryCapacity(4L);
        host.setHypervisorType("KVM");
        host.setDescription("example");
        host.setNqn("nqn.2014-08.org.nvmexpress:uuid:748d0363-8366-44db-803b-146effb96988");
        event.setInventory(host);
        return event;
    }
}
