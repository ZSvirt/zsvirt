package org.zstack.externalbackup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by MaJin on 2019/12/3.
 */

@RestResponse(fieldsTo = "inventory")
public class APICreateExternalBackupEvent extends APIEvent {
    private ExternalBackupInventory inventory;

    public ExternalBackupInventory getInventory() {
        return inventory;
    }

    public void setInventory(ExternalBackupInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateExternalBackupEvent(String apiId) {
        super(apiId);
    }

    public APICreateExternalBackupEvent() {
        super();
    }

    public static APICreateExternalBackupEvent __example__() {
        APICreateExternalBackupEvent event = new APICreateExternalBackupEvent();
        event.setInventory(ExternalBackupInventory.__example__());
        return event;
    }
}
