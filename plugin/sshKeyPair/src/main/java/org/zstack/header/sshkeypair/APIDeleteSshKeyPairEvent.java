package org.zstack.header.sshkeypair;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteSshKeyPairEvent extends APIEvent {
    public APIDeleteSshKeyPairEvent(){}

    public APIDeleteSshKeyPairEvent(String msgId) { super(msgId); }

    public static APIDeleteSshKeyPairEvent __example__() {
        APIDeleteSshKeyPairEvent event = new APIDeleteSshKeyPairEvent();
        event.setSuccess(true);
        return event;
    }
}
