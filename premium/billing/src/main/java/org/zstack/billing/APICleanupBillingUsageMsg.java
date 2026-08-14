package org.zstack.billing;

import org.springframework.http.HttpMethod;
import org.zstack.billing.table.PriceTableVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/billings/usage",
        method = HttpMethod.DELETE,
        responseClass = APICleanupBillingUsageEvent.class
)
public class APICleanupBillingUsageMsg extends APIDeleteMessage {
    public static APICleanupBillingUsageMsg __example__() {
        APICleanupBillingUsageMsg msg = new APICleanupBillingUsageMsg();
        return msg;
    }
}
