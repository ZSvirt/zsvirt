package org.zstack.sns.platform.dingtalk;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveSNSDingTalkAtPersonEvent extends APIEvent {
    public APIRemoveSNSDingTalkAtPersonEvent() {
    }

    public APIRemoveSNSDingTalkAtPersonEvent(String apiId) {
        super(apiId);
    }
}
