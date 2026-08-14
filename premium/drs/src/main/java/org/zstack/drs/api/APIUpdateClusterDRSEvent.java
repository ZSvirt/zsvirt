package org.zstack.drs.api;

import org.zstack.drs.entity.ClusterDRSInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by lining on 2019/12/12.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateClusterDRSEvent extends APIEvent {
    private ClusterDRSInventory inventory;

    public ClusterDRSInventory getInventory() {
        return inventory;
    }

    public void setInventory(ClusterDRSInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateClusterDRSEvent() {
    }

    public APIUpdateClusterDRSEvent(String apiId) {
        super(apiId);
    }

}
