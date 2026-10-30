package org.zstack.header.identity;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vo.ResourceVO;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/4/8.
 */
@RestResponse(allTo = "inventories")
public class APIGetResourceAccountReply extends APIReply {
    private Map<String, AccountInventory> inventories;

    public Map<String, AccountInventory> getInventories() {
        return inventories;
    }

    public void setInventories(Map<String, AccountInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetResourceAccountReply __example__() {
        APIGetResourceAccountReply reply = new APIGetResourceAccountReply();

        Map<String, AccountInventory> inventories = new HashMap<>();
        inventories.put(uuid(ResourceVO.class), AccountInventory.__example__());
        reply.setInventories(inventories);

        return reply;
    }

}
