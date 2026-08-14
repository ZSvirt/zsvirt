package org.zstack.pciDevice.virtual.sr_iov;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.sriov.EthernetVfPciDeviceInventory;
import org.zstack.header.sriov.EthernetVfStatus;
import org.zstack.pciDevice.PciDeviceAddress;
import org.zstack.pciDevice.PciDeviceType;


import java.util.Arrays;
import java.util.List;

/**
 * Created by shixin.ruan on 12/19/2023.
 */
@RestResponse(allTo = "inventories")
public class APIQueryEthernetVFReply extends APIQueryReply {
    private List<EthernetVfPciDeviceInventory> inventories;

    public List<EthernetVfPciDeviceInventory>  getInventories() {
        return inventories;
    }

    public void setInventories(List<EthernetVfPciDeviceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryEthernetVFReply __example__() {
        APIQueryEthernetVFReply reply = new APIQueryEthernetVFReply();
        EthernetVfPciDeviceInventory inv = new EthernetVfPciDeviceInventory("eth0");

        inv.setVfStatus(EthernetVfStatus.Available);
        inv.setHostUuid(uuid());
        inv.setParentUuid(uuid());
        inv.setParentUuid(uuid());
        inv.setType(PciDeviceType.Ethernet_Controller);
        inv.setVendorId("10de");
        inv.setDeviceId("0e0f");
        inv.setSubdeviceId("118b");
        inv.setSubvendorId("10de");
        inv.setPciDeviceAddress(new PciDeviceAddress("06:00.1").toString());

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
