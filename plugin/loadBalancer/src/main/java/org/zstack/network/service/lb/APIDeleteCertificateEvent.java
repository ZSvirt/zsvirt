package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by shixin on 03/22/2018.
 */
@RestResponse
public class APIDeleteCertificateEvent extends APIEvent {
    public APIDeleteCertificateEvent() {
    }

    public APIDeleteCertificateEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteCertificateEvent __example__() {
        APIDeleteCertificateEvent event = new APIDeleteCertificateEvent();
        event.setSuccess(true);
        return event;
    }

}
