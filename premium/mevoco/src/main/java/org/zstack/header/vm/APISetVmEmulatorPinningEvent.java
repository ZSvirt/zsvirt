package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by longtao.wu@zstack.io on 21/12/01
 */
@RestResponse
public class APISetVmEmulatorPinningEvent extends APIEvent {
    public APISetVmEmulatorPinningEvent() {
    }

    public APISetVmEmulatorPinningEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmEmulatorPinningEvent __example__() {
        return new APISetVmEmulatorPinningEvent();
    }

}
