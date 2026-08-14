package org.zstack.header.search;

import org.zstack.header.message.APIEvent;

public class APIDeleteSearchIndexEvent extends APIEvent {
    public APIDeleteSearchIndexEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteSearchIndexEvent() {
        super(null);
    }
 
    public static APIDeleteSearchIndexEvent __example__() {
        APIDeleteSearchIndexEvent event = new APIDeleteSearchIndexEvent();


        return event;
    }

}
