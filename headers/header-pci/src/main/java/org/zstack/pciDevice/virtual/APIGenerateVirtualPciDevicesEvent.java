package org.zstack.pciDevice.virtual;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-04-24.
 */
@RestResponse
public class APIGenerateVirtualPciDevicesEvent extends APIEvent {
    public APIGenerateVirtualPciDevicesEvent() {
    }

    public APIGenerateVirtualPciDevicesEvent(String apiId) {
        super(apiId);
    }

    public static APIGenerateVirtualPciDevicesEvent __example__() {
        return new APIGenerateVirtualPciDevicesEvent();
    }
}
