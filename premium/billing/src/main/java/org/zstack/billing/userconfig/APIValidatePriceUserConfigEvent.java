package org.zstack.billing.userconfig;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/4/10.
 */

@RestResponse
public class APIValidatePriceUserConfigEvent extends APIEvent {
    public APIValidatePriceUserConfigEvent() {
        super(null);
    }

    public APIValidatePriceUserConfigEvent(String apiId) {
        super(apiId);
    }
}
