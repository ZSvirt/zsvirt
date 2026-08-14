package org.zstack.usbDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by GuoYi on 10/21/17.
 */
@RestRequest(
        path = "/usb-device/usb-devices/{usbDeviceUuid}/attach",
        method = HttpMethod.POST,
        responseClass = APIAttachUsbDeviceToVmEvent.class,
        parameterName = "params"
)
public class APIAttachUsbDeviceToVmMsg extends APIMessage {
    @APIParam(resourceType = UsbDeviceVO.class, scope = APIParam.SCOPE_ALLOWED_SHARING)
    private String usbDeviceUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(required = false, validValues = {"PassThrough","Redirect"})
    private String attachType = "PassThrough";

    public static APIAttachUsbDeviceToVmMsg __example__() {
        APIAttachUsbDeviceToVmMsg msg = new APIAttachUsbDeviceToVmMsg();
        msg.setUsbDeviceUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        msg.setAttachType(UsbAttachType.PassThrough.toString());
        return msg;
    }

    public String getUsbDeviceUuid() {
        return usbDeviceUuid;
    }

    public void setUsbDeviceUuid(String usbDeviceUuid) {
        this.usbDeviceUuid = usbDeviceUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getAttachType() {
        return attachType;
    }

    public void setAttachType(String attachType) {
        this.attachType = attachType;
    }
}
