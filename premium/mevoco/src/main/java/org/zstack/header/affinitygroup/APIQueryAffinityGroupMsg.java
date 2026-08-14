package org.zstack.header.affinitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created with IntelliJ IDEA.
 * User: shixin
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
@AutoQuery(replyClass = APIQueryAffinityGroupReply.class, inventoryClass = AffinityGroupInventory.class)
@RestRequest(
        path = "/affinity-groups",
        optionalPaths = {"/affinity-groups/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAffinityGroupReply.class
)
public class APIQueryAffinityGroupMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }

}
