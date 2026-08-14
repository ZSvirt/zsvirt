package org.zstack.ha;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/3/28.
 */
@RestResponse
public class APISetVmInstanceHaLevelEvent extends APIEvent {
    public APISetVmInstanceHaLevelEvent() {
    }

    public APISetVmInstanceHaLevelEvent(String apiId) {
        super(apiId);
    }
 
    public static APISetVmInstanceHaLevelEvent __example__() {
        APISetVmInstanceHaLevelEvent event = new APISetVmInstanceHaLevelEvent();


        return event;
    }

}
