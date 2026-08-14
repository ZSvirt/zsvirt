package org.zstack.header.sshkeypair;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachSshKeyPairFromVmInstanceEvent extends APIEvent {
    public APIDetachSshKeyPairFromVmInstanceEvent() {}

    public APIDetachSshKeyPairFromVmInstanceEvent(String msgId) { super(msgId); }

    public static APIDetachSshKeyPairFromVmInstanceEvent __example__() {
        APIDetachSshKeyPairFromVmInstanceEvent event = new APIDetachSshKeyPairFromVmInstanceEvent();
        event.setSuccess(true);
        return event;
    }
}
