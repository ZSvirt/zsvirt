package org.zstack.header.longjob;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 11/13/17.
 */
@RestResponse
public class APICancelLongJobEvent extends APIEvent {
    public APICancelLongJobEvent() {
    }

    public APICancelLongJobEvent(String apiId) {
        super(apiId);
    }

    public static APICancelLongJobEvent __example__() {
        APICancelLongJobEvent event = new APICancelLongJobEvent();
        event.setSuccess(true);
        return event;
    }
}
