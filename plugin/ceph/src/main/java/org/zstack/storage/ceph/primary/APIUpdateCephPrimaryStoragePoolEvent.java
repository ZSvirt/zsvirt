package org.zstack.storage.ceph.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/9/4.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateCephPrimaryStoragePoolEvent extends APIEvent{
    private CephPrimaryStoragePoolInventory inventory;

    public APIUpdateCephPrimaryStoragePoolEvent() {
    }

    public APIUpdateCephPrimaryStoragePoolEvent(String apiId) {
        super(apiId);
    }

    public CephPrimaryStoragePoolInventory getInventory() {
        return inventory;
    }

    public void setInventory(CephPrimaryStoragePoolInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateCephPrimaryStoragePoolEvent __example__() {
        APIUpdateCephPrimaryStoragePoolEvent event = new APIUpdateCephPrimaryStoragePoolEvent();

        CephPrimaryStoragePoolInventory inv = new CephPrimaryStoragePoolInventory();
        inv.setUuid(uuid());
        inv.setPoolName("pool name");
        inv.setAliasName("alias name");
        inv.setDescription("description");
        inv.setType(CephPrimaryStoragePoolType.Data.toString());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setPrimaryStorageUuid(uuid());
        event.setInventory(inv);
        return event;
    }

}
