package org.zstack.sns.platform.feishu;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveSNSFeiShuAtPersonEvent extends APIEvent {
    public APIRemoveSNSFeiShuAtPersonEvent() {
    }

    public APIRemoveSNSFeiShuAtPersonEvent(String apiId) {
        super(apiId);
    }
}
