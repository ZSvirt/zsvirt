package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
@AutoQuery(inventoryClass = StackTemplateInventory.class, replyClass = APIQueryStackTemplateReply.class)
@RestRequest(
        path = "/cloudformation/template",
        optionalPaths = {"/cloudformation/template/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryStackTemplateReply.class
)
public class APIQueryStackTemplateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=test");
    }
}
