package org.zstack.drs.api;

import org.zstack.drs.entity.ClusterDRSInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by lining on 2019/12/12.
 */
@RestResponse(allTo = "inventory")
public class APICreateClusterDRSEvent extends APIEvent {
    private ClusterDRSInventory inventory;

    public ClusterDRSInventory getInventory() {
        return inventory;
    }

    public void setInventory(ClusterDRSInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateClusterDRSEvent(String apiId) {
        super(apiId);
    }

    public APICreateClusterDRSEvent() {
        super(null);
    }
}
