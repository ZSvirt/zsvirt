package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;


@RestRequest(
        path = "/volumes/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVolumeIoThreadPinEvent.class
)
public class APISetVolumeIoThreadPinMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmUuid;

    @APIParam
    private String pin;

    @APIParam(numberRange = {1, 10})
    private int ioThreadId;

    public void setIoThreadId(int ioThreadId) {
        this.ioThreadId = ioThreadId;
    }

    public int getIoThreadId() {
        return ioThreadId;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPin() {
        return pin;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getVolumeUuid() {
        return getUuid();
    }

    public static APISetVolumeIoThreadPinMsg __example__() {
        APISetVolumeIoThreadPinMsg msg = new APISetVolumeIoThreadPinMsg();
        msg.setVmUuid(uuid());
        msg.setUuid(uuid());
        msg.setPin("3-6");
        msg.setIoThreadId(1);

        return msg;
    }
}
