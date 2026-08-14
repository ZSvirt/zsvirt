package org.zstack.storage.device.iscsi;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/2
 */

@RestResponse
public class APIDeleteIscsiServerEvent extends APIEvent {
    public APIDeleteIscsiServerEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteIscsiServerEvent() {
    }

    public static APIDeleteIscsiServerEvent __example__() {
        APIDeleteIscsiServerEvent event = new APIDeleteIscsiServerEvent();
        return event;
    }
}
