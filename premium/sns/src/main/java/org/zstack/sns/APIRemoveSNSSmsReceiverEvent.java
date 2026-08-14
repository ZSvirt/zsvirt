package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Qi Le on 2019-07-11
 */
@RestResponse
public class APIRemoveSNSSmsReceiverEvent extends APIEvent {
    public APIRemoveSNSSmsReceiverEvent() {
    }

    public APIRemoveSNSSmsReceiverEvent(String apiId) {
        super(apiId);
    }
}
