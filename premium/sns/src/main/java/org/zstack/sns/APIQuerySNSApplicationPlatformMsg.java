package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSApplicationPlatformReply.class, inventoryClass = SNSApplicationPlatformInventory.class)
@RestRequest(path = "/sns/application-platforms", optionalPaths = {"/sns/application-platforms/{uuid}"},
        responseClass = APIQuerySNSApplicationPlatformReply.class, method = HttpMethod.GET)
public class APIQuerySNSApplicationPlatformMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("state=Enabled");
    }
}
