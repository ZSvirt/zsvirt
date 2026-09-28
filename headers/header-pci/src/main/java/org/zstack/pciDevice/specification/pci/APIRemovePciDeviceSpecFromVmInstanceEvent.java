package org.zstack.pciDevice.specification.pci;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-05-22.
 */
@RestResponse
public class APIRemovePciDeviceSpecFromVmInstanceEvent extends APIEvent {
    public APIRemovePciDeviceSpecFromVmInstanceEvent() {
    }

    public APIRemovePciDeviceSpecFromVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public static APIRemovePciDeviceSpecFromVmInstanceEvent __example__() {
        return new APIRemovePciDeviceSpecFromVmInstanceEvent();
    }
}
