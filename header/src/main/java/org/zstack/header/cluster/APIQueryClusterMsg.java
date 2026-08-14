package org.zstack.header.cluster;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryClusterReply.class, inventoryClass = ClusterInventory.class)
@RestRequest(
        path = "/clusters",
        optionalPaths = {"/clusters/{uuid}"},
        responseClass = APIQueryClusterReply.class,
        method = HttpMethod.GET
)
public class APIQueryClusterMsg extends APIQueryMessage {

 
    public static List<String> __example__() {
            return asList("hypervisorType=KVM");
    }

}
