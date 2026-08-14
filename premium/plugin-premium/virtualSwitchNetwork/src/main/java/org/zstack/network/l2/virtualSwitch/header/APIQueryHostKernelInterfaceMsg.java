package org.zstack.network.l2.virtualSwitch.header;

import java.util.List;
import static java.util.Arrays.asList;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryHostKernelInterfaceReply.class, inventoryClass = HostKernelInterfaceInventory.class)
@RestRequest(
        path = "/l3-networks/kernel-interfaces",
        optionalPaths = {"/l3-networks/kernel-interfaces/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryHostKernelInterfaceReply.class
)
public class APIQueryHostKernelInterfaceMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}
