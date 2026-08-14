package org.zstack.templateConfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryTemplateConfigReply.class, inventoryClass = TemplateConfigInventory.class)
@RestRequest(
        path = "/template-configurations/configs",
        optionalPaths = {"/template-configurations/configs/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryTemplateConfigReply.class
)
public class APIQueryTemplateConfigMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
