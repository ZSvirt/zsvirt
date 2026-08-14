package org.zstack.header.storage.addon.primary;

import org.zstack.header.log.MaskSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;

import java.sql.Timestamp;
import java.util.LinkedHashMap;

@RestResponse(allTo = "inventory")
@MaskSensitiveInfo
public class APIUpdateExternalPrimaryStorageEvent extends APIEvent {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private ExternalPrimaryStorageInventory inventory;

    public APIUpdateExternalPrimaryStorageEvent(String id) {
        super(id);
    }

    public APIUpdateExternalPrimaryStorageEvent() {
        super();
    }

    public void setInventory(ExternalPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }

    public ExternalPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public static APIUpdateExternalPrimaryStorageEvent __example__() {
        APIUpdateExternalPrimaryStorageEvent event = new APIUpdateExternalPrimaryStorageEvent();
        ExternalPrimaryStorageInventory inv = new ExternalPrimaryStorageInventory();
        inv.setUuid(uuid());
        inv.setUrl("http://operator:*****@172.25.1.5:80");
        inv.setIdentity("expon");
        inv.setConfig(JSONObjectUtil.toObject("{\"pools\":[{\"name\":\"pool1\",\"aliasName\":\"pool-high\"},{\"name\":\"pool2\"}]}", LinkedHashMap.class));
        inv.setAddonInfo(JSONObjectUtil.toObject("{\"pools\":[{\"name\":\"pool1\",\"availableCapacity\":100,\"totalCapacity\":200},{\"name\":\"pool2\",\"availableCapacity\":100,\"totalCapacity\":200}]}", LinkedHashMap.class));
        inv.setTotalCapacity(SizeUnit.GIGABYTE.toByte(100L));
        inv.setAvailableCapacity(SizeUnit.GIGABYTE.toByte(50L));
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
