package org.zstack.usbDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 10/21/17.
 */
@AutoQuery(replyClass = APIQueryUsbDeviceReply.class, inventoryClass = UsbDeviceInventory.class)
@RestRequest(
        path = "/usb-device/usb-devices",
        optionalPaths = {"/usb-device/usb-devices/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryUsbDeviceReply.class
)
public class APIQueryUsbDeviceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Arrays.asList();
    }
}
