package org.zstack.sns.platform.http;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSHttpEndpointReply.class, inventoryClass = SNSHttpEndpointInventory.class)
@RestRequest(path = "/sns/application-endpoints/http", optionalPaths = {"/sns/application-endpoints/http/{uuid}"},
        responseClass = APIQuerySNSHttpEndpointReply.class, method = HttpMethod.GET)
public class APIQuerySNSHttpEndpointMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("url=http://url");
    }
}
