package org.zstack.billing.userconfig;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/4/10.
 */

@RestResponse
public class APIValidateInstanceOfferingUserConfigEvent extends APIEvent {
    public APIValidateInstanceOfferingUserConfigEvent() {
        super(null);
    }

    public APIValidateInstanceOfferingUserConfigEvent(String apiId) {
        super(apiId);
    }
}
