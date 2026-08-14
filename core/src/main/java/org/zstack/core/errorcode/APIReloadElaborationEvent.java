package org.zstack.core.errorcode;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
@RestResponse
public class APIReloadElaborationEvent extends APIEvent {
    public static APIReloadElaborationEvent __example__() {
        APIReloadElaborationEvent ret = new APIReloadElaborationEvent();
        return ret;
    }

    public APIReloadElaborationEvent() {
    }

    public APIReloadElaborationEvent(String apiId) {
        super(apiId);
    }
}
