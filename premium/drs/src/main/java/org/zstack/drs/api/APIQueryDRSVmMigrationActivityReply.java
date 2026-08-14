package org.zstack.drs.api;

import org.zstack.drs.entity.DRSVmMigrationActivityInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by lining on 2019/12/12.
 */
@RestResponse(allTo = "inventories")
public class APIQueryDRSVmMigrationActivityReply extends APIQueryReply {
    private List<DRSVmMigrationActivityInventory> inventories;

    public List<DRSVmMigrationActivityInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<DRSVmMigrationActivityInventory> inventories) {
        this.inventories = inventories;
    }
}
