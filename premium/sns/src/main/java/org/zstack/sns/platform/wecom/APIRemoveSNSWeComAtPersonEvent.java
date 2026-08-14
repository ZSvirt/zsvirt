package org.zstack.sns.platform.wecom;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveSNSWeComAtPersonEvent extends APIEvent {
    public APIRemoveSNSWeComAtPersonEvent() {
    }

    public APIRemoveSNSWeComAtPersonEvent(String apiId) {
        super(apiId);
    }
}
