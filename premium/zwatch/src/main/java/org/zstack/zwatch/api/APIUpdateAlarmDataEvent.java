package org.zstack.zwatch.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2018/11/13.
 */
@RestResponse
public class APIUpdateAlarmDataEvent extends APIEvent {

    public APIUpdateAlarmDataEvent() {

    }

    public APIUpdateAlarmDataEvent(String apiId) {
        super(apiId);
    }
}
