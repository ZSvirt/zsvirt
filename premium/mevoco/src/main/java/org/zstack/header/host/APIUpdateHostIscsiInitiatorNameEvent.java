package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.kvm.KVMHostInventory;

@RestResponse(fieldsTo = {"all"})
public class APIUpdateHostIscsiInitiatorNameEvent extends APIEvent {
    private KVMHostInventory inventory;

    public APIUpdateHostIscsiInitiatorNameEvent() { super(null); }

    public APIUpdateHostIscsiInitiatorNameEvent(String apiId) {
        super(apiId);
    }

    public KVMHostInventory getInventory() {
        return inventory;
    }

    public void setInventory(KVMHostInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateHostIscsiInitiatorNameEvent __example__() {
        APIUpdateHostIscsiInitiatorNameEvent event = new APIUpdateHostIscsiInitiatorNameEvent();
        KVMHostInventory host = new KVMHostInventory();
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
        host.setIscsiInitiatorName("iqn.2015-01.io.helix:a6e4508d2378");
        event.setInventory(host);
        return event;
    }
}
