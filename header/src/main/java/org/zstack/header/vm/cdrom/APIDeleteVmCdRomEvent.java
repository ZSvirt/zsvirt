package org.zstack.header.vm.cdrom;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Create by lining at 2018/12/27
 */
@RestResponse
public class APIDeleteVmCdRomEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public APIDeleteVmCdRomEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteVmCdRomEvent() {
        super(null);
    }
 
    public static APIDeleteVmCdRomEvent __example__() {
        return new APIDeleteVmCdRomEvent();
    }
}
