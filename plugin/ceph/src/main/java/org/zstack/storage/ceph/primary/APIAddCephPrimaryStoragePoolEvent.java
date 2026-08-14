package org.zstack.storage.ceph.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.ceph.DataSecurityPolicy;

import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/2/28.
 */
@RestResponse(allTo = "inventory")
public class APIAddCephPrimaryStoragePoolEvent extends APIEvent {
    public APIAddCephPrimaryStoragePoolEvent() {
    }

    public APIAddCephPrimaryStoragePoolEvent(String apiId) {
        super(apiId);
    }

    private CephPrimaryStoragePoolInventory inventory;

    public CephPrimaryStoragePoolInventory getInventory() {
        return inventory;
    }

    public void setInventory(CephPrimaryStoragePoolInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddCephPrimaryStoragePoolEvent __example__() {
        APIAddCephPrimaryStoragePoolEvent event = new APIAddCephPrimaryStoragePoolEvent();

        CephPrimaryStoragePoolInventory inv = new CephPrimaryStoragePoolInventory();
        inv.setUuid(uuid());
        inv.setPoolName("pool name");
        inv.setAliasName("alias name");
        inv.setDescription("description");
        inv.setType(CephPrimaryStoragePoolType.Data.toString());
        inv.setReplicatedSize(3);
        inv.setSecurityPolicy(DataSecurityPolicy.Copy.toString());
        inv.setDiskUtilization(0.33f);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setPrimaryStorageUuid(uuid());
        event.setInventory(inv);
        return event;
    }
    
}