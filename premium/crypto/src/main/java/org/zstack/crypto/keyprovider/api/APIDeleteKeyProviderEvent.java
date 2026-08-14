package org.zstack.crypto.keyprovider.api;

import org.zstack.header.message.APIEvent;

public abstract class APIDeleteKeyProviderEvent extends APIEvent {
    public APIDeleteKeyProviderEvent() {
        super(null);
    }

    public APIDeleteKeyProviderEvent(String apiId) {
        super(apiId);
    }

}
