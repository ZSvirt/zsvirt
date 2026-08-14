package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-05-22.
 */
@RestResponse
public class APIRemoveMdevDeviceSpecFromVmInstanceEvent extends APIEvent {
    public APIRemoveMdevDeviceSpecFromVmInstanceEvent() {
    }

    public APIRemoveMdevDeviceSpecFromVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public static APIRemoveMdevDeviceSpecFromVmInstanceEvent __example__() {
        return new APIRemoveMdevDeviceSpecFromVmInstanceEvent();
    }
}
