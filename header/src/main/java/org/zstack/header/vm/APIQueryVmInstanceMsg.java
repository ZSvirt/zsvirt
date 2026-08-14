package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVmInstanceReply.class, inventoryClass = VmInstanceInventory.class)
@RestRequest(
        path = "/vm-instances",
        optionalPaths = {"/vm-instances/{uuid}"},
        responseClass = APIQueryVmInstanceReply.class,
        method = HttpMethod.GET
)
public class APIQueryVmInstanceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=vm1", "vmNics.ip=192.168.20.100");
    }
}
