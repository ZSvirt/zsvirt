package org.zstack.header.vo;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.zone.ZoneVO;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/5/1.
 */
@RestResponse(allTo = "inventories")
public class APIGetResourceNamesReply extends APIReply {
    private List<ResourceInventory> inventories;

    public List<ResourceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ResourceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetResourceNamesReply __example__() {
        APIGetResourceNamesReply reply = new APIGetResourceNamesReply();
        ResourceInventory inv = new ResourceInventory();
        inv.setUuid(uuid());
        inv.setResourceName("zone");
        inv.setResourceType(ZoneVO.class.getSimpleName());

        ResourceInventory inv1 = new ResourceInventory();
        inv1.setUuid(uuid());
        inv1.setResourceName("vm");
        inv1.setResourceType(VmInstanceVO.class.getSimpleName());

        reply.setInventories(asList(inv, inv1));
        return reply;
    }
}
