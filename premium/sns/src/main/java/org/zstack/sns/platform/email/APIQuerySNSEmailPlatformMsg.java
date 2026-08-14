package org.zstack.sns.platform.email;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSEmailPlatformReply.class, inventoryClass = SNSEmailPlatformInventory.class)
@RestRequest(path = "/sns/application-platforms/email", optionalPaths = {"/sns/application-platforms/email/{uuid}"},
        responseClass = APIQuerySNSEmailPlatformReply.class, method = HttpMethod.GET)
public class APIQuerySNSEmailPlatformMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=email");
    }
}
