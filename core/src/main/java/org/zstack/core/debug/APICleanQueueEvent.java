package org.zstack.core.debug;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.APIDeleteVmNicEvent;

/**
 * Created by LiangHanYu on 2021/5/21 11:28
 */
@RestResponse
public class APICleanQueueEvent extends APIEvent {
    public APICleanQueueEvent(String apiId) {
        super(apiId);
    }

    public APICleanQueueEvent() {
        super(null);
    }


    public static APIDeleteVmNicEvent __example__() {
        APIDeleteVmNicEvent event = new APIDeleteVmNicEvent();
        return event;
    }
}
