package org.zstack.templateConfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryGlobalConfigTemplateReply.class, inventoryClass = GlobalConfigTemplateInventory.class)
@RestRequest(
        path = "/template-configurations/templates",
        optionalPaths = {"/template-configurations/templates/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryGlobalConfigTemplateReply.class
)

public class APIQueryGlobalConfigTemplateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
