package org.zstack.header.longjob;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 12/7/17.
 */
@RestResponse
public class APIDeleteLongJobEvent extends APIEvent {
    public APIDeleteLongJobEvent() {
    }

    public APIDeleteLongJobEvent(String apiId) {
        super(apiId);
    }

    public static APICancelLongJobEvent __example__() {
        APICancelLongJobEvent event = new APICancelLongJobEvent();
        return event;
    }
}
