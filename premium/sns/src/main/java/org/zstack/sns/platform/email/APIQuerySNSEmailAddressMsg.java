package org.zstack.sns.platform.email;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSEmailAddressReply.class, inventoryClass = SNSEmailAddressInventory.class)
@RestRequest(path = "/sns/application-endpoints/emails/email-addresses",
        optionalPaths = {"/sns/application-endpoints/emails/email-addresses/{uuid}"},
        responseClass = APIQuerySNSEmailAddressReply.class, method = HttpMethod.GET)
public class APIQuerySNSEmailAddressMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList(String.format("uuid=%s", uuid()), String.format("endpointUuid=%s", uuid()));
    }
}
