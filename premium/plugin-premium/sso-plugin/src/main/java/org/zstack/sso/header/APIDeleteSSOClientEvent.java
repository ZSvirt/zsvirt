package org.zstack.sso.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/8/30
 */
@RestResponse
public class APIDeleteSSOClientEvent extends APIEvent {
    public APIDeleteSSOClientEvent() {
    }

    public APIDeleteSSOClientEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteSSOClientEvent __example__() {
        APIDeleteSSOClientEvent evt = new APIDeleteSSOClientEvent();
        return evt;
    }
}
