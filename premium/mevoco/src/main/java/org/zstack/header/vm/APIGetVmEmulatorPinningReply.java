package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by longtao.wu@zstack.io on 21/12/01
 */
@RestResponse(fieldsTo = {"emulatorPinning"})
public class APIGetVmEmulatorPinningReply extends APIReply {
    private String emulatorPinning;

    public String getEmulatorPinning() {
        return emulatorPinning;
    }

    public void setEmulatorPinning(String emulatorPinning) {
        this.emulatorPinning = emulatorPinning;
    }

    public static APIGetVmEmulatorPinningReply __example__() {
        APIGetVmEmulatorPinningReply reply = new APIGetVmEmulatorPinningReply();
        reply.setEmulatorPinning("1-4");
        return reply;
    }

}
