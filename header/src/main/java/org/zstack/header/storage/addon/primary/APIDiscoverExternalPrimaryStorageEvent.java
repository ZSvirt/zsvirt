package org.zstack.header.storage.addon.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;

import java.sql.Timestamp;
import java.util.LinkedHashMap;

@RestResponse(allTo = "inventory")
public class APIDiscoverExternalPrimaryStorageEvent extends APIEvent {
    private ExternalPrimaryStorageInventory inventory;

    public ExternalPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ExternalPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }

    public APIDiscoverExternalPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public APIDiscoverExternalPrimaryStorageEvent() {
        super();
    }

    public static APIDiscoverExternalPrimaryStorageEvent __example__() {
        APIDiscoverExternalPrimaryStorageEvent event = new APIDiscoverExternalPrimaryStorageEvent();
        ExternalPrimaryStorageInventory inv = new ExternalPrimaryStorageInventory();
        inv.setUuid(uuid());
        inv.setUrl("http://operator:*****@172.25.1.5:80");
        inv.setIdentity("expon");
        inv.setAddonInfo(JSONObjectUtil.toObject("{\"pools\":[{\"name\":\"pool1\",\"availableCapacity\":100,\"totalCapacity\":200},{\"name\":\"pool2\",\"availableCapacity\":100,\"totalCapacity\":200}]}", LinkedHashMap.class));
        inv.setTotalCapacity(SizeUnit.GIGABYTE.toByte(100L));
        inv.setAvailableCapacity(SizeUnit.GIGABYTE.toByte(50L));
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
