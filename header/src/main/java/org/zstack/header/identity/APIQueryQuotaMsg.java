package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by frank on 7/14/2015.
 */
@AutoQuery(replyClass = APIQueryQuotaReply.class, inventoryClass = QuotaInventory.class)
@RestRequest(
        path = "/accounts/quotas",
        method = HttpMethod.GET,
        responseClass = APIQueryQuotaReply.class
)
public class APIQueryQuotaMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("name=test");
    }

}
