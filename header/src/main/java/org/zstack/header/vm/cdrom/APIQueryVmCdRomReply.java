package org.zstack.header.vm.cdrom;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.sql.Timestamp;
import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryVmCdRomReply extends APIQueryReply {
    private List<VmCdRomInventory> inventories;

    public List<VmCdRomInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmCdRomInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmCdRomReply __example__() {
        APIQueryVmCdRomReply reply = new APIQueryVmCdRomReply();

        VmCdRomInventory inventory = new VmCdRomInventory();
        inventory.setName("cd-1");
        inventory.setUuid(uuid());
        inventory.setDescription("desc");
        inventory.setIsoUuid(uuid());
        inventory.setDeviceId(0);
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(inventory));
        return reply;
    }
}
