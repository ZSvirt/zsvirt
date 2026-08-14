package org.zstack.crypto.keyprovider.nkp.api;

import org.zstack.crypto.keyprovider.api.APIDeleteKeyProviderEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteNkpEvent extends APIDeleteKeyProviderEvent {
    public APIDeleteNkpEvent() {
        super(null);
    }

    public APIDeleteNkpEvent(String apiId) {
        super(apiId);
    }
}
