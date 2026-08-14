package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIRegisterVmInstanceFromMetadataEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APIRegisterVmInstanceFromMetadataEvent() {
        super(null);
    }

    public APIRegisterVmInstanceFromMetadataEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIRegisterVmInstanceFromMetadataEvent __example__() {
        APIRegisterVmInstanceFromMetadataEvent evt = new APIRegisterVmInstanceFromMetadataEvent();
        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setUuid(uuid());
        vm.setName("recovered-vm");
        evt.setInventory(vm);
        return evt;
    }
}
