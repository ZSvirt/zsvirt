package org.zstack.header.zone;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;


/**
 * @apiResult api event for message :ref:`APICreateZoneMsg`
 * @example {
 * "org.zstack.header.zone.APICreateZoneEvent": {
 * "inventory": {
 * "uuid": "1fdbe4af825149d6b49d567f272ddbab",
 * "name": "zone1",
 * "description": "Test",
 * "state": "Enabled",
 * "type": "zstack",
 * "createDate": "Mar 24, 2014 7:32:03 AM",
 * "lastOpDate": "Mar 24, 2014 7:32:03 AM"
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APICreateZoneEvent extends APIEvent {
    /**
     * @desc zone inventory (see :ref:`ZoneInventory`)
     */
    private ZoneInventory inventory;

    public APICreateZoneEvent() {
        super(null);
    }

    public APICreateZoneEvent(String apiId) {
        super(apiId);
    }

    public ZoneInventory getInventory() {
        return inventory;
    }

    public void setInventory(ZoneInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateZoneEvent __example__() {
        APICreateZoneEvent event = new APICreateZoneEvent();
        ZoneInventory zone = new ZoneInventory();
        zone.setName("TestZone");
        zone.setUuid(uuid());
        zone.setDescription("Test");
        zone.setState(ZoneState.Enabled.toString());
        zone.setType("zstack");
        zone.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        zone.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(zone);
        return event;
    }

}
