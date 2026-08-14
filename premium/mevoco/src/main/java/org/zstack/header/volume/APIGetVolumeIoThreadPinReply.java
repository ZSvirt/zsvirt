package org.zstack.header.volume;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;


@RestResponse(fieldsTo = {"volumeUuid", "ioThreadId", "pin"})
public class APIGetVolumeIoThreadPinReply extends APIReply {
    private String volumeUuid;
    private String pin;
    private String ioThreadId;

    public void setIoThreadId(String ioThreadId) {
        this.ioThreadId = ioThreadId;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getIoThreadId() {
        return ioThreadId;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public String getPin() {
        return pin;
    }

    public static APIGetVolumeIoThreadPinReply __example__() {
        APIGetVolumeIoThreadPinReply msg = new APIGetVolumeIoThreadPinReply();
        msg.setVolumeUuid(uuid());
        msg.setIoThreadId("1");
        msg.setPin("3-6");
        return msg;
    }
}
