package org.zstack.header.vpc.ha;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
@AutoQuery(replyClass = APIQueryVpcHaGroupReply.class, inventoryClass = VpcHaGroupInventory.class)
@RestRequest(
        path = "/vpc/hagroups",
        optionalPaths = {"/vpc/hagroups/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVpcHaGroupReply.class
)
public class APIQueryVpcHaGroupMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }

}
