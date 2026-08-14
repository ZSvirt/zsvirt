package org.zstack.usbDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 10/21/17.
 */
@RestRequest(
        path = "/usb-device/usb-devices/{usbDeviceUuid}/detach",
        method = HttpMethod.POST,
        responseClass = APIDetachUsbDeviceFromVmEvent.class,
        parameterName = "params"
)
public class APIDetachUsbDeviceFromVmMsg extends APIMessage {
    @APIParam(resourceType = UsbDeviceVO.class, scope = APIParam.SCOPE_ALLOWED_SHARING)
    private String usbDeviceUuid;

    public static APIDetachUsbDeviceFromVmMsg __example__() {
        APIDetachUsbDeviceFromVmMsg msg = new APIDetachUsbDeviceFromVmMsg();
        msg.setUsbDeviceUuid(uuid());
        return msg;
    }

    public String getUsbDeviceUuid() {
        return usbDeviceUuid;
    }

    public void setUsbDeviceUuid(String usbDeviceUuid) {
        this.usbDeviceUuid = usbDeviceUuid;
    }
}
