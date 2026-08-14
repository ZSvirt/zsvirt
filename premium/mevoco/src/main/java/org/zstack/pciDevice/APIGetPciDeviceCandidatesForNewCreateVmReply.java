package org.zstack.pciDevice;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 02/09/2017
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetPciDeviceCandidatesForNewCreateVmReply extends APIReply {
    private List<PciDeviceInventory> inventories;

    public List<PciDeviceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PciDeviceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetPciDeviceCandidatesForNewCreateVmReply __example__() {
        APIGetPciDeviceCandidatesForNewCreateVmReply reply = new APIGetPciDeviceCandidatesForNewCreateVmReply();
        PciDeviceInventory inv = new PciDeviceInventory();
        inv.setUuid(uuid());
        inv.setHostUuid(uuid());
        inv.setStatus(PciDeviceStatus.Attached);
        inv.setType(PciDeviceType.GPU_Video_Controller);
        inv.setVendorId("10de");
        inv.setDeviceId("0e0f");
        inv.setSubdeviceId("118b");
        inv.setSubvendorId("10de");
        inv.setPciDeviceAddress(new PciDeviceAddress("06:00.1").toString());

        reply.setInventories(asList(inv));
        reply.setSuccess(true);
        return reply;
    }
}
