package org.zstack.vpc;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * created by boce.wang 24/06/2022
 */
@RestResponse
public class APIUpdateVirtualRouterSoftwareVersionEvent extends APIEvent {

    public APIUpdateVirtualRouterSoftwareVersionEvent() {
    }

    public APIUpdateVirtualRouterSoftwareVersionEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateVirtualRouterSoftwareVersionEvent __example__() {
        APIUpdateVirtualRouterSoftwareVersionEvent ret = new APIUpdateVirtualRouterSoftwareVersionEvent();
        return ret;
    }
}
