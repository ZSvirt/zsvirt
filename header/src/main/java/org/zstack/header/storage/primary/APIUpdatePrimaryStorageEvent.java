package org.zstack.header.storage.primary;

import org.zstack.header.log.MaskSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(allTo = "inventory")
@MaskSensitiveInfo
public class APIUpdatePrimaryStorageEvent extends APIEvent {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private PrimaryStorageInventory inventory;

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdatePrimaryStorageEvent() {
    }

    public APIUpdatePrimaryStorageEvent(String apiId) {
        super(apiId);
    }
 
    public static APIUpdatePrimaryStorageEvent __example__() {
        APIUpdatePrimaryStorageEvent event = new APIUpdatePrimaryStorageEvent();

        PrimaryStorageInventory ps = new PrimaryStorageInventory();
        ps.setName("New PS1");
        ps.setUrl("/zstack_ps");
        ps.setType("LocalStorage");
        ps.setAttachedClusterUuids(Collections.singletonList(uuid()));
        ps.setState(PrimaryStorageState.Enabled.toString());
        ps.setStatus(PrimaryStorageStatus.Connected.toString());

        event.setInventory(ps);
        return event;
    }

}
