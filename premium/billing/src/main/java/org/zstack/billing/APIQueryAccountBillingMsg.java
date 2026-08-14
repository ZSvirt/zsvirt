package org.zstack.billing;

import org.springframework.http.HttpMethod;
import org.zstack.billing.generator.BillingInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by lining on 2019/5/27.
 */
@AutoQuery(inventoryClass = BillingInventory.class, replyClass = APIQueryAccountBillingReply.class)
@RestRequest(
        path = "/billing/billings",
        optionalPaths = {"/billing/billings/{id}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAccountBillingReply.class
)
public class APIQueryAccountBillingMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("accountUuid=" + uuid());
    }

}
