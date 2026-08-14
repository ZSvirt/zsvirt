package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/6/14.
 */
@AutoQuery(inventoryClass = CloudFormationStackEventInventory.class, replyClass = APIQueryEventFromResourceStackReply.class)
@RestRequest(
        path = "/cloudformation/event",
        optionalPaths = {"/cloudformation/event/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryEventFromResourceStackReply.class
)
public class APIQueryEventFromResourceStackMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=test");
    }
}
