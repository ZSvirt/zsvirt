package org.zstack.header.protocol;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICreateVRouterOspfAreaEvent extends APIEvent {
    private RouterAreaInventory inventory;

    public APICreateVRouterOspfAreaEvent() {
        super(null);
    }

    public APICreateVRouterOspfAreaEvent(String apiId) {
        super(apiId);
    }

    public RouterAreaInventory getInventory() {
        return inventory;
    }

    public void setInventory(RouterAreaInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateVRouterOspfAreaEvent __example__() {
        APICreateVRouterOspfAreaEvent event = new APICreateVRouterOspfAreaEvent();
        RouterAreaInventory area = new RouterAreaInventory();
        area.setUuid(uuid());
        area.setType(RouterAreaType.Stub.toString());
        area.setAreaId("1");
        area.setAuthentication(RouterAreaAuthType.Plaintext.toString());
        area.setPassword("password");
        area.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        area.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(area);

        return event;
    }
}
