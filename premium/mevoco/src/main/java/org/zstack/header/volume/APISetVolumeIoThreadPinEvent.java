package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"volumeUuid", "ioThreadId", "pin"})
public class APISetVolumeIoThreadPinEvent extends APIEvent {
    private String volumeUuid;
    private String pin;
    private Integer ioThreadId;

    public APISetVolumeIoThreadPinEvent() {
        super(null);
    }

    public APISetVolumeIoThreadPinEvent(String appId) {
        super(appId);
    }

    public void setIoThreadId(Integer ioThreadId) {
        this.ioThreadId = ioThreadId;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public Integer getIoThreadId() {
        return ioThreadId;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public String getPin() {
        return pin;
    }

    public static APISetVolumeIoThreadPinEvent __example__() {
        APISetVolumeIoThreadPinEvent evt = new APISetVolumeIoThreadPinEvent();
        evt.setVolumeUuid(uuid());
        evt.setIoThreadId(1);
        evt.setPin("3-6");
        return evt;
    }
}
