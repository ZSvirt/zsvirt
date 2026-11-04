package org.zstack.mttyDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author yu.sun
 * @date 2022/11/22 14:17
 **/
@RestResponse
public class APIGenerateSeMdevDevicesEvent extends APIEvent {
    public APIGenerateSeMdevDevicesEvent() {
    }

    public APIGenerateSeMdevDevicesEvent(String apiId) {
        super(apiId);
    }

    public static APIGenerateSeMdevDevicesEvent __example__() {
        return new APIGenerateSeMdevDevicesEvent();
    }
}