package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.network.l3.APIAddDnsToL3NetworkEvent;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

@RestResponse
public class APIDeleteRoleEvent extends APIEvent {
    public APIDeleteRoleEvent() {
    }

    public APIDeleteRoleEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteRoleEvent __example__() {
        APIDeleteRoleEvent event = new APIDeleteRoleEvent();
        return event;
    }
}
