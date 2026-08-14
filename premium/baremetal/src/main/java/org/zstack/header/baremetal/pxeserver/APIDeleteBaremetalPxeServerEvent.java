package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2017/5/7.
 */
@RestResponse
public class APIDeleteBaremetalPxeServerEvent extends APIEvent {
    public APIDeleteBaremetalPxeServerEvent() {
    }

    public APIDeleteBaremetalPxeServerEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteBaremetalPxeServerEvent __example__() {
        return new APIDeleteBaremetalPxeServerEvent();
    }
}
