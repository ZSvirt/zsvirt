package org.zstack.usbDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 10/24/2017.
 */
@RestRequest(
        path = "/usb-device/usb-devices/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateUsbDeviceEvent.class,
        isAction = true
)
public class APIUpdateUsbDeviceMsg extends APIMessage {
    @APIParam(resourceType = UsbDeviceVO.class)
    private String uuid;

    @APIParam(required = false, maxLength = 2048)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, validValues = {"Enabled", "Disabled"})
    private String state;

    public static APIUpdateUsbDeviceMsg __example__() {
        APIUpdateUsbDeviceMsg msg = new APIUpdateUsbDeviceMsg();
        msg.setUuid(uuid());
        msg.setName("usb");
        msg.setDescription("this is a usb device");
        msg.setState(UsbDeviceState.Enabled.toString());

        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
