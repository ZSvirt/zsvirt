package org.zstack.header.protocol;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIUpdateVRouterOspfAreaEvent extends APIEvent {
    private RouterAreaInventory inventory;

    public APIUpdateVRouterOspfAreaEvent() {
        super(null);
    }

    public APIUpdateVRouterOspfAreaEvent(String apiId) {
        super(apiId);
    }

    public RouterAreaInventory getInventory() {
        return inventory;
    }

    public void setInventory(RouterAreaInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateVRouterOspfAreaEvent __example__() {
        APIUpdateVRouterOspfAreaEvent event = new APIUpdateVRouterOspfAreaEvent();
        RouterAreaInventory area = new RouterAreaInventory();
        area.setUuid(uuid());
        area.setType(RouterAreaType.Stub.toString());
        area.setAreaId("1");
        area.setAuthentication(RouterAreaAuthType.MD5.toString());
        area.setKeyId(128);
        area.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        area.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(area);

        return event;
    }
}
