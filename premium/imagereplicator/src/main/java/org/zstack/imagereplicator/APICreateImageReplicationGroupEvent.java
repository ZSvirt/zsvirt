package org.zstack.imagereplicator;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICreateImageReplicationGroupEvent extends APIEvent {
    private ImageReplicationGroupInventory inventory;

    public ImageReplicationGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageReplicationGroupInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateImageReplicationGroupEvent() {
        super(null);
    }

    public APICreateImageReplicationGroupEvent(String apiId) {
        super(apiId);
    }

    public static APICreateImageReplicationGroupEvent __example__() {
        APICreateImageReplicationGroupEvent evt = new APICreateImageReplicationGroupEvent();
        ImageReplicationGroupInventory inv = new ImageReplicationGroupInventory();
        inv.setName("testgroup");
        inv.setDescription("my test group");
        inv.setState(ReplicationGroupState.Enabled);
        inv.setUuid(uuid());
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        evt.setInventory(inv);
        return evt;
    }
}
