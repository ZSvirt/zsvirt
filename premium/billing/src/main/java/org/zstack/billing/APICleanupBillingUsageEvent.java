package org.zstack.billing;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APICleanupBillingUsageEvent extends APIEvent {
    public APICleanupBillingUsageEvent() {
    }

    public APICleanupBillingUsageEvent(String apiId) {
        super(apiId);
    }
 
    public static APICleanupBillingUsageEvent __example__() {
        APICleanupBillingUsageEvent event = new APICleanupBillingUsageEvent();


        return event;
    }

}
