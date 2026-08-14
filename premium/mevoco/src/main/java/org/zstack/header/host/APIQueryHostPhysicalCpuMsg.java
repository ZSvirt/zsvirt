package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@Action(category = HostConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQueryHostPhysicalCpuReply.class, inventoryClass = HostPhysicalCpuInventory.class)
@RestRequest(
        path = "/hosts/physical-cpu",
        optionalPaths = {"/hosts/physical-cpu/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryHostPhysicalCpuReply.class
)
public class APIQueryHostPhysicalCpuMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.emptyList();
    }
}
