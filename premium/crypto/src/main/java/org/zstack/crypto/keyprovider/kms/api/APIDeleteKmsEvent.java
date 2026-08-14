package org.zstack.crypto.keyprovider.kms.api;

import org.zstack.crypto.keyprovider.api.APIDeleteKeyProviderEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteKmsEvent extends APIDeleteKeyProviderEvent {
    public APIDeleteKmsEvent() {
        super(null);
    }

    public APIDeleteKmsEvent(String apiId) {
        super(apiId);
    }
}
