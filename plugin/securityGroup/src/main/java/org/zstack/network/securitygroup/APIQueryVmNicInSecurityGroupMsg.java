package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVmNicInSecurityGroupReply.class, inventoryClass = VmNicSecurityGroupRefInventory.class)
@RestRequest(
        path = "/security-groups/vm-instances/nics",
        method = HttpMethod.GET,
        responseClass = APIQueryVmNicInSecurityGroupReply.class
)
public class APIQueryVmNicInSecurityGroupMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}
