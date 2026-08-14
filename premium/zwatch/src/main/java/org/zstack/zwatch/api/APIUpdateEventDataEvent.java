package org.zstack.zwatch.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2018/11/13.
 */
@RestResponse
public class APIUpdateEventDataEvent extends APIEvent {

    public APIUpdateEventDataEvent() {

    }

    public APIUpdateEventDataEvent(String apiId) {
        super(apiId);
    }
}
