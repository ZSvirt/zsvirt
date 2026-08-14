package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * author:kaicai.hu
 * Date:2021/8/6
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetHostPhysicalMemoryFactsReply extends APIReply {
    List<HostPhysicalMemoryInventory> inventories;

    public List<HostPhysicalMemoryInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostPhysicalMemoryInventory> inventories) {
        this.inventories = inventories;
    }


    public static APIGetHostPhysicalMemoryFactsReply __example__() {
        APIGetHostPhysicalMemoryFactsReply reply = new APIGetHostPhysicalMemoryFactsReply();

        HostPhysicalMemoryInventory inventory = new HostPhysicalMemoryInventory();
        inventory.setUuid(uuid());
        inventory.setHostUuid(uuid());
        inventory.setManufacturer("Micron");
        inventory.setSize("32 GB");
        inventory.setSpeed("2933 MT/s");
        inventory.setClockSpeed("2933 MT/s");
        inventory.setLocator("CPU0_CH0_DIMM0");
        inventory.setSerialNumber("2B8573C1");
        inventory.setRank("2");
        inventory.setVoltage("1.2 V");
        inventory.setType("DDR4");
        inventory.setCreateDate(new Timestamp(DocUtils.date));
        inventory.setLastOpDate(new Timestamp(DocUtils.date));

        reply.setInventories(Arrays.asList(inventory));
        return reply;
    }
}
