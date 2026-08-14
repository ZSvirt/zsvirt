package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APICreateSystemTagsEvent extends APIEvent {
    private List<SystemTagInventory> inventories;

    public APICreateSystemTagsEvent(String apiId) {
        super(apiId);
    }

    public APICreateSystemTagsEvent() {
        super(null);
    }

    public List<SystemTagInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SystemTagInventory> inventories) {
        this.inventories = inventories;
    }

    public static APICreateSystemTagsEvent __example__() {
        APICreateSystemTagsEvent event = new APICreateSystemTagsEvent();
        SystemTagInventory tag = new SystemTagInventory();
        tag.setInherent(false);
        tag.setType("System");
        tag.setResourceType(uuid());
        tag.setResourceType("HostVO");
        tag.setTag("reservedMemory::1G");
        tag.setUuid(uuid()  );
        tag.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        tag.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventories(Collections.singletonList(tag));
        return event;
    }
}
