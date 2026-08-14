package org.zstack.drs.api;

import org.zstack.drs.entity.ClusterDRSInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lining on 2019/12/12.
 */
@RestResponse(allTo = "inventories")
public class APIQueryClusterDRSReply extends APIQueryReply {
    private List<ClusterDRSInventory> inventories;

    public List<ClusterDRSInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ClusterDRSInventory> inventories) {
        this.inventories = inventories;
    }
}
