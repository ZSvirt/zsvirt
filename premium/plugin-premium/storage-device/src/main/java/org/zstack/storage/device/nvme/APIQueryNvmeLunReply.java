package org.zstack.storage.device.nvme;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryNvmeLunReply extends APIQueryReply {
    private List<NvmeLunInventory> inventories;

    public List<NvmeLunInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NvmeLunInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryNvmeLunReply __example__() {
        APIQueryNvmeLunReply reply = new APIQueryNvmeLunReply();

        NvmeLunInventory nvmeLunInv11 = new NvmeLunInventory();
        nvmeLunInv11.setName("nvme-lun-36b083fe000daf018000022905ba35d8f");
        nvmeLunInv11.setNvmeTargetUuid(uuid());
        nvmeLunInv11.setUuid(uuid());
        nvmeLunInv11.setWwn("uuid.48daeab7-7f15-405e-8481-7152cb9b0aca");
        nvmeLunInv11.setType("disk");
        nvmeLunInv11.setSerial("3d87eca1686c1782");
        nvmeLunInv11.setSize(5497558138880L);
        nvmeLunInv11.setWwid("uuid.48daeab7-7f15-405e-8481-7152cb9b0aca");
        nvmeLunInv11.setPath("nvme-uuid.48daeab7-7f15-405e-8481-7152cb9b0aca ");


        reply.setInventories(Collections.singletonList(nvmeLunInv11));
        return reply;
    }
}
