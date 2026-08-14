package org.zstack.pciDevice.gpu;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/8 18:28
 */
@AutoQuery(replyClass = APIQueryGpuDeviceReply.class, inventoryClass = GpuDeviceInventory.class)
@RestRequest(
        path = "/gpu-device/gpu-devices",
        optionalPaths = {"/gpu-device/gpu-devices/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryGpuDeviceReply.class
)
public class APIQueryGpuDeviceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
