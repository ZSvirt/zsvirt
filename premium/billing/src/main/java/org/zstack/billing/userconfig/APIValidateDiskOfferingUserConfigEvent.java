package org.zstack.billing.userconfig;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/4/10.
 */

@RestResponse
public class APIValidateDiskOfferingUserConfigEvent extends APIEvent {
    public APIValidateDiskOfferingUserConfigEvent() {
        super(null);
    }

    public APIValidateDiskOfferingUserConfigEvent(String apiId) {
        super(apiId);
    }
}
