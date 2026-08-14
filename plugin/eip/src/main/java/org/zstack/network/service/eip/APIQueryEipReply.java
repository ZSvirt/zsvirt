package org.zstack.network.service.eip;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmNicInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryEipReply extends APIQueryReply {
    private List<EipInventory> inventories;

    public List<EipInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<EipInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryEipReply __example__() {
        APIQueryEipReply reply = new APIQueryEipReply();

        EipInventory eip = new EipInventory();
        eip.setName("Test-EIP");
        eip.setVipUuid(uuid());
        eip.setVmNicUuid(uuid());
        eip.setState(EipState.Enabled.toString());

        reply.setInventories(asList(eip));
        return reply;
    }

}
