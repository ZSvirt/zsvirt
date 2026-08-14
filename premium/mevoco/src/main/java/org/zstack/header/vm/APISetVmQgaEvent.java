package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 17/1/4.
 */
@RestResponse
public class APISetVmQgaEvent extends APIEvent {
	public APISetVmQgaEvent() {
	}

	public APISetVmQgaEvent(String apiId) {
		super(apiId);
	}

	public static APISetVmQgaEvent __example__() {
		APISetVmQgaEvent event = new APISetVmQgaEvent();


		return event;
	}

}
