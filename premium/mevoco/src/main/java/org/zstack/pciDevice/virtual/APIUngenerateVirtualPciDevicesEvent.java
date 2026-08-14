package org.zstack.pciDevice.virtual;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-04-24.
 */
@RestResponse
public class APIUngenerateVirtualPciDevicesEvent extends APIEvent {
    public APIUngenerateVirtualPciDevicesEvent() {
    }

    public APIUngenerateVirtualPciDevicesEvent(String apiId) {
        super(apiId);
    }

    public static APIUngenerateVirtualPciDevicesEvent __example__() {
        return new APIUngenerateVirtualPciDevicesEvent();
    }
}
