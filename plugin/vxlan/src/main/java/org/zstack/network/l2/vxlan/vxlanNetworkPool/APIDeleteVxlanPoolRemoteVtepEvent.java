package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.l2.vxlan.vtep.RemoteVtepInventory;
@RestResponse
public class APIDeleteVxlanPoolRemoteVtepEvent extends APIEvent {


    public APIDeleteVxlanPoolRemoteVtepEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteVxlanPoolRemoteVtepEvent() {
        super(null);
    }

 
    public static APIDeleteVxlanPoolRemoteVtepEvent __example__() {
        APIDeleteVxlanPoolRemoteVtepEvent event = new APIDeleteVxlanPoolRemoteVtepEvent();
        return event;
    }

}
