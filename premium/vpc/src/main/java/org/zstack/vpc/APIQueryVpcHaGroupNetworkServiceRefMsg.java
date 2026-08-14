package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.ha.VpcHaGroupNetworkServiceRefInventory;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVpcHaGroupNetworkServiceRefReply.class, inventoryClass = VpcHaGroupNetworkServiceRefInventory.class)
@RestRequest(
        path = "/vpc/hagroups/networkserviceref/",
        optionalPaths = {"/vpc/hagroups/networkserviceref/{uuid}"},
        responseClass = APIQueryVpcHaGroupNetworkServiceRefReply.class,
        method = HttpMethod.GET
)
public class APIQueryVpcHaGroupNetworkServiceRefMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=vpcHaGroupNetworkServiceRef");
    }
}
