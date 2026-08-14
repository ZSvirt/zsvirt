package org.zstack.header.acl;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/
@RestResponse
public class APIDeleteAccessControlListEvent extends APIEvent {
    public APIDeleteAccessControlListEvent() {
    }

    public APIDeleteAccessControlListEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteAccessControlListEvent __example__() {
        APIDeleteAccessControlListEvent event = new APIDeleteAccessControlListEvent();
        event.setSuccess(true);
        return event;
    }

}