package org.zstack.billing.generator;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/4/10.
 */

@RestResponse
public class APIGenerateAccountBillingEvent extends APIEvent {
    public APIGenerateAccountBillingEvent() {
        super(null);
    }

    public APIGenerateAccountBillingEvent(String apiId) {
        super(apiId);
    }
}
