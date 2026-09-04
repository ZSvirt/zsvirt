package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIDestroyVmInstanceMsg`
 * @example {
 * "org.zstack.header.vm.APIDestroyVmInstanceEvent": {
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse
public class APIDestroyVmInstanceEvent extends APIEvent {

    public APIDestroyVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public APIDestroyVmInstanceEvent() {
        super(null);
    }
 
    public static APIDestroyVmInstanceEvent __example__() {
        return new APIDestroyVmInstanceEvent();
    }
}
