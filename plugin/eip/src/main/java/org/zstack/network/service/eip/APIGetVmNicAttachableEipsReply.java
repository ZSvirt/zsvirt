package org.zstack.network.service.eip;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIGetVmNicAttachableEipsReply extends APIReply {
    private List<EipInventory> inventories;

    public List<EipInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<EipInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVmNicAttachableEipsReply __example__() {
        APIGetVmNicAttachableEipsReply reply = new APIGetVmNicAttachableEipsReply();

        EipInventory eip = new EipInventory();
        eip.setName("eip");

        Timestamp time = new Timestamp(org.zstack.header.message.DocUtils.date);

        eip.setUuid(uuid());
        eip.setVmNicUuid("d29fb63d5aba4bbf98ec8663d6b4ba21");
        eip.setVipUuid("a5cfe85eb0d7448fbe153360c52114ce");
        eip.setState("Enable");
        eip.setVipIp("10.72.109.11");
        eip.setGuestIp("172.168.1.1");
        eip.setCreateDate(time);
        eip.setLastOpDate(time);

        reply.setInventories(asList(eip));

        return reply;
    }
}

