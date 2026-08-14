package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by Qi Le on 2019-07-23
 */
@AutoQuery(replyClass = APIQuerySNSSmsEndpointReply.class, inventoryClass = SNSSmsEndpointInventory.class)
@RestRequest(
        path = "/sns/sms-endpoints",
        optionalPaths = {"/sns/sms-endpoints/{uuid}"},
        responseClass = APIQuerySNSSmsEndpointReply.class,
        method = HttpMethod.GET
)
public class APIQuerySNSSmsEndpointMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=SmsEndpoint");
    }
}
