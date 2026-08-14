package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by frank on 8/17/2015.
 */

@RestResponse(allTo = "inventory")
public class APIUpdateSystemTagEvent extends APIEvent {
    private SystemTagInventory inventory;

    public APIUpdateSystemTagEvent() {
    }

    public APIUpdateSystemTagEvent(String apiId) {
        super(apiId);
    }

    public SystemTagInventory getInventory() {
        return inventory;
    }

    public void setInventory(SystemTagInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateSystemTagEvent __example__() {
        APIUpdateSystemTagEvent event = new APIUpdateSystemTagEvent();
        SystemTagInventory tag = new SystemTagInventory();
        tag.setInherent(false);
        tag.setType("System");
        tag.setResourceType(uuid());
        tag.setResourceType("HostVO");
        tag.setTag("reservedMemory::1G");
        tag.setUuid(uuid()  );
        tag.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        tag.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(tag);
        event.setSuccess(true);
        return event;
    }

}
