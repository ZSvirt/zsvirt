package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

import static java.util.Arrays.asList;

/**
 * Created by shixin.ruan on 2020/02/12.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateVirtualRouterEvent extends APIEvent {
    private VirtualRouterVmInventory inventory;

    public APIUpdateVirtualRouterEvent() {
    }

    public APIUpdateVirtualRouterEvent(String apiId) {
        super(apiId);
    }

    public VirtualRouterVmInventory getInventory() {
        return inventory;
    }

    public void setInventory(VirtualRouterVmInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateVirtualRouterEvent __example__() {
        APIUpdateVirtualRouterEvent event = new APIUpdateVirtualRouterEvent();

        VirtualRouterVmInventory vr = new VirtualRouterVmInventory();

        vr.setName("Test-Router");
        vr.setDescription("this is a virtual router vm");
        vr.setClusterUuid(uuid());
        vr.setImageUuid(uuid());
        vr.setInstanceOfferingUuid(uuid());
        vr.setManagementNetworkUuid(uuid());
        vr.setPublicNetworkUuid(uuid());

        event.setInventory(vr);

        return event;
    }

}
