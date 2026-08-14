package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/6/11.
 */
@AutoQuery(inventoryClass = ResourceStackInventory.class, replyClass = APIQueryResourceStackReply.class)
@RestRequest(
        path = "/cloudformation/stack",
        optionalPaths = {"/cloudformation/stack/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryResourceStackReply.class
)
public class APIQueryResourceStackMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=test");
    }
}
