package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSApplicationEndpointReply.class, inventoryClass = SNSApplicationEndpointInventory.class)
@RestRequest(path = "/sns/application-endpoints", optionalPaths = {"/sns/application-endpoints/{uuid}"},
        responseClass = APIQuerySNSApplicationEndpointReply.class, method = HttpMethod.GET)
public class APIQuerySNSApplicationEndpointMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=http");
    }
}
