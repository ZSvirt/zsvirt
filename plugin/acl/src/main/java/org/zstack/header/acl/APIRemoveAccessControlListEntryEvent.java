package org.zstack.header.acl;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-10
 **/
@RestResponse
public class APIRemoveAccessControlListEntryEvent extends APIEvent {
    public APIRemoveAccessControlListEntryEvent() {
    }

    public APIRemoveAccessControlListEntryEvent(String apiId) {
        super(apiId);
    }

    public static APIRemoveAccessControlListEntryEvent __example__() {
        APIRemoveAccessControlListEntryEvent event = new APIRemoveAccessControlListEntryEvent();
        event.setSuccess(true);
        return event;
    }

}