package org.zstack.header.host;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryHostPhysicalCpuReply extends APIQueryReply {
    private List<HostPhysicalCpuInventory> inventories;

    public List<HostPhysicalCpuInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostPhysicalCpuInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryHostPhysicalCpuReply __example__() {
        APIQueryHostPhysicalCpuReply reply = new APIQueryHostPhysicalCpuReply();

        HostPhysicalCpuInventory inventory = new HostPhysicalCpuInventory();
        inventory.setUuid(uuid());
        inventory.setHostUuid(uuid());
        inventory.setSerialNumber("DE71C8539FA93551");
        inventory.setVersion("Intel(R) Xeon(R) Gold 5318Y CPU @ 2.10GHz");
        inventory.setCurrentSpeed("2100 MHz");
        inventory.setCoreCount(24);
        inventory.setThreadCount(48);
        inventory.setCreateDate(new Timestamp(DocUtils.date));
        inventory.setLastOpDate(new Timestamp(DocUtils.date));

        reply.setInventories(Arrays.asList(inventory));
        return reply;
    }
}
