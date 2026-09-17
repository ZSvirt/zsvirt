package org.zstack.ha;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/3/29.
 */
@RestResponse
public class APIDeleteVmInstanceHaLevelEvent extends APIEvent {

    public APIDeleteVmInstanceHaLevelEvent() {
    }

    public APIDeleteVmInstanceHaLevelEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteVmInstanceHaLevelEvent __example__() {
        APIDeleteVmInstanceHaLevelEvent event = new APIDeleteVmInstanceHaLevelEvent();
        return event;
    }
}
