package org.zstack.nas;

import org.zstack.header.identity.APIDeleteAccountEvent;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/3/5.
 */
@RestResponse
public class APIDeleteNasFileSystemEvent extends APIEvent {
    public APIDeleteNasFileSystemEvent() {
    }

    public APIDeleteNasFileSystemEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteNasFileSystemEvent __example__() {
        APIDeleteNasFileSystemEvent event = new APIDeleteNasFileSystemEvent();


        return event;
    }
}
