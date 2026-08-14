package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVmNicSecurityPolicyReply.class, inventoryClass = VmNicSecurityPolicyInventory.class)
@RestRequest(
        path = "/security-groups/nics/security-policy",
        optionalPaths = {"/security-groups/nics/{uuid}/security-policy"},
        method = HttpMethod.GET,
        responseClass = APIQueryVmNicSecurityPolicyReply.class
)

public class APIQueryVmNicSecurityPolicyMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("ingressPolicy=DROP", "egressPolicy=DROP");
    }
}
