package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vpc.VpcSnatStateInventory;
import java.util.List;
import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVpcSnatStateReply.class, inventoryClass = VpcSnatStateInventory.class)
@RestRequest(
        path = "/vpc/virtual-routers/networkservicestate/snat",
        optionalPaths = {"/vpc/virtual-routers/networkservicestate/snat/{uuid}"},
        responseClass = APIQueryVpcSnatStateReply.class,
        method = HttpMethod.GET
)
public class APIQueryVpcSnatStateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=vpcSnatState");
    }
}
