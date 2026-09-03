package org.zstack.sso.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/9/6
 */
@RestResponse
public class APIDeleteSSORedirectTemplateEvent extends APIEvent {
    public APIDeleteSSORedirectTemplateEvent() {
    }

    public APIDeleteSSORedirectTemplateEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteSSORedirectTemplateEvent __example__() {
        APIDeleteSSORedirectTemplateEvent evt = new APIDeleteSSORedirectTemplateEvent();
        return evt;
    }
}
