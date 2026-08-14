package org.zstack.header.storage.primary;

import org.zstack.header.log.MaskSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

/**
 * @apiResult api event for message of *addPrimaryStorage*, for example, :ref:`APIAddNfsPrimaryStorageMsg`
 * @example {
 * "org.zstack.header.storage.primary.APIAddPrimaryStorageEvent": {
 * "inventory": {
 * "uuid": "53f83279ae7244d1953f92a64335b23b",
 * "zoneUuid": "22c8ddfd70b44aed903fb5d4023a6a84",
 * "name": "nfs",
 * "url": "nfs://test",
 * "description": "Test",
 * "totalCapacity": 0,
 * "availableCapacity": 0,
 * "type": "NFS",
 * "state": "Enabled",
 * "mountPath": "/opt/zstack/nfsprimarystorage/prim-53f83279ae7244d1953f92a64335b23b",
 * "createDate": "May 1, 2014 12:04:18 AM",
 * "lastOpDate": "May 1, 2014 12:04:18 AM",
 * "attachedClusterUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
@MaskSensitiveInfo
public class APIAddPrimaryStorageEvent extends APIEvent {
    /**
     * @desc see :ref:`PrimaryStorageInventory`
     */

    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private PrimaryStorageInventory inventory;

    public APIAddPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public APIAddPrimaryStorageEvent() {
        super(null);
    }

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddPrimaryStorageEvent __example__() {
        APIAddPrimaryStorageEvent event = new APIAddPrimaryStorageEvent();

        PrimaryStorageInventory ps = new PrimaryStorageInventory();
        ps.setName("PS1");
        ps.setUrl("/zstack_ps");
        ps.setType("LocalStorage");
        ps.setAttachedClusterUuids(Collections.singletonList(uuid()));
        ps.setState(PrimaryStorageState.Enabled.toString());
        ps.setStatus(PrimaryStorageStatus.Connected.toString());

        event.setInventory(ps);
        return event;
    }

}
