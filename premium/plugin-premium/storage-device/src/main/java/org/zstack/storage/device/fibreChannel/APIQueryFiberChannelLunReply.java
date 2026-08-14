package org.zstack.storage.device.fibreChannel;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.device.iscsi.IscsiLunInventory;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryFiberChannelLunReply extends APIQueryReply {
    private List<FiberChannelLunInventory> inventories;

    public List<FiberChannelLunInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<FiberChannelLunInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryFiberChannelLunReply __example__() {
        APIQueryFiberChannelLunReply reply = new APIQueryFiberChannelLunReply();

        FiberChannelLunInventory fiberChannelLunInventory = new FiberChannelLunInventory();
        fiberChannelLunInventory.setName("fc-lun-36b083fe000daf018000022905ba35d8f");
        fiberChannelLunInventory.setFiberChannelStorageUuid(uuid());
        fiberChannelLunInventory.setUuid(uuid());
        fiberChannelLunInventory.setWwn("0x6f01faf000d5c3e7");
        fiberChannelLunInventory.setModel("MD32xx");
        fiberChannelLunInventory.setVendor("DELL");
        fiberChannelLunInventory.setType("mpath");
        fiberChannelLunInventory.setSerial("6b083fe000daf018000015505abbe00a");
        fiberChannelLunInventory.setSize(5497558138880l);
        fiberChannelLunInventory.setWwid("36b083fe000daf018000022905ba35d8f");
        fiberChannelLunInventory.setPath("pci-0000:05:00.0-fc-0x2012b083fedaf018-lun-11");

        FiberChannelLunInventory fiberChannelLunInventory2 = new FiberChannelLunInventory();
        fiberChannelLunInventory2.setName("fc-lun-36f01faf000d5c3e7000023ef5ba362f2");
        fiberChannelLunInventory2.setFiberChannelStorageUuid(uuid());
        fiberChannelLunInventory2.setUuid(uuid());
        fiberChannelLunInventory2.setWwn("0x6b083fe000daf018");
        fiberChannelLunInventory2.setModel("MD32xx");
        fiberChannelLunInventory2.setVendor("DELL");
        fiberChannelLunInventory2.setType("mpath");
        fiberChannelLunInventory2.setSerial("6b083fe000daf018000015505abbe00a");
        fiberChannelLunInventory2.setSize(5497558138880l);
        fiberChannelLunInventory2.setWwid("36f01faf000d5c3e7000023ef5ba362f2");
        fiberChannelLunInventory2.setPath("pci-0000:05:00.0-fc-0x2012b083fedaf018-lun-12");

        reply.setInventories(asList(fiberChannelLunInventory, fiberChannelLunInventory2));
        reply.setSuccess(true);
        return reply;
    }
}
