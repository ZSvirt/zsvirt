package org.zstack.header.core.progress;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import java.util.List;
import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/3/21.
 */
@RestResponse(allTo = "inventories")
public class APIGetTaskProgressReply extends APIReply {
    private List<TaskProgressInventory> inventories;

    public List<TaskProgressInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TaskProgressInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetTaskProgressReply __example__() {
        APIGetTaskProgressReply msg = new APIGetTaskProgressReply();
        msg.setInventories(asList(TaskProgressInventory.__example__()));
        return msg;
    }

}