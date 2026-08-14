package org.zstack.storage.device.hba;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.springframework.http.HttpMethod;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/20 11:49
 */
@AutoQuery(replyClass = APIQueryFcHbaDeviceReply.class, inventoryClass = FcHbaDeviceInventory.class)
@RestRequest(
        path = "/storage-devices/hba",
        optionalPaths = {"/storage-devices/hba/{uuid}"},
        responseClass = APIQueryFcHbaDeviceReply.class,
        method = HttpMethod.GET
)
public class APIQueryFcHbaDeviceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid="+ uuid());
    }
}
