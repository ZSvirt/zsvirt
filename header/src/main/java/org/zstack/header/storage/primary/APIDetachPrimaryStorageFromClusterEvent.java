package org.zstack.header.storage.primary;

import org.zstack.header.log.MaskSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIDetachPrimaryStorageMsg`
 * @example {
 * "org.zstack.header.storage.primary.APIDetachPrimaryStorageEvent": {
 * "inventory": {
 * "uuid": "f4ac0a3119c94c6fae844c2298615d27",
 * "zoneUuid": "f04caf351c014aa890126fc78193d063",
 * "name": "SimulatorPrimaryStorage-0",
 * "url": "nfs://simulator/primary/-0",
 * "description": "Test Primary Storage",
 * "totalCapacity": 10995116277760,
 * "availableCapacity": 10995116277760,
 * "type": "SimulatorPrimaryStorage",
 * "state": "Enabled",
 * "mountPath": "/primarystoragesimulator/f4ac0a3119c94c6fae844c2298615d27",
 * "createDate": "May 1, 2014 2:42:51 PM",
 * "lastOpDate": "May 1, 2014 2:42:51 PM",
 * "attachedClusterUuids": [
 * "f23e402bc53b4b5abae87273b6004016",
 * "4a1789235a86409a9a6db83f97bc582f",
 * "fe755538d4e845d5b82073e4f80cb90b",
 * "1f45d6d6c02b43bfb6196dcacb5b8a25"
 * ]
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
@MaskSensitiveInfo
public class APIDetachPrimaryStorageFromClusterEvent extends APIEvent {
    public APIDetachPrimaryStorageFromClusterEvent() {
        super(null);
    }

    public APIDetachPrimaryStorageFromClusterEvent(String apiId) {
        super(apiId);
    }

    /**
     * @desc see :ref:`PrimaryStorageInventory`
     */
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private PrimaryStorageInventory inventory;

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIDetachPrimaryStorageFromClusterEvent __example__() {
        APIDetachPrimaryStorageFromClusterEvent event = new APIDetachPrimaryStorageFromClusterEvent();

        PrimaryStorageInventory ps = new PrimaryStorageInventory();
        ps.setName("PS1");
        ps.setUrl("/zstack_ps");
        ps.setType("LocalStorage");
        ps.setState(PrimaryStorageState.Enabled.toString());
        ps.setStatus(PrimaryStorageStatus.Connected.toString());

        event.setInventory(ps);
        return event;
    }

}
