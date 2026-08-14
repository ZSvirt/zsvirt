package org.zstack.header.volume;

public class IoThreadPinStruct {
    public int ioThreadId;
    public String pin;
    public String volumeUuid;


    public int getIoThreadId() {
        return ioThreadId;
    }

    public void setIoThreadId(int ioThreadId) {
        this.ioThreadId = ioThreadId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }
}
