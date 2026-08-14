package org.zstack.core.config;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIResetGlobalConfigEvent extends APIEvent {

	public APIResetGlobalConfigEvent(String apiId) {
	    super(apiId);
    }
	public APIResetGlobalConfigEvent() {
		super(null);
	}
 
    public static APIResetGlobalConfigEvent __example__() {
        APIResetGlobalConfigEvent event = new APIResetGlobalConfigEvent();
		event.setSuccess(true);
        return event;
    }

}
