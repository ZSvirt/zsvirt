package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * author:kaicai.hu
 * Date:2021/8/9
 */
@AutoQuery(replyClass = APIQueryHostPhysicalMemoryReply.class, inventoryClass = HostPhysicalMemoryInventory.class)
@RestRequest(
        path = "/hosts/physicalmemory",
        optionalPaths = {"/hosts/physicalmemory/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryHostPhysicalMemoryReply.class
)
public class APIQueryHostPhysicalMemoryMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.emptyList();
    }
}
