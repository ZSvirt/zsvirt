package org.zstack.ldap.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteLdapBindingEvent extends APIEvent {
    public APIDeleteLdapBindingEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteLdapBindingEvent() {
        super(null);
    }
 
    public static APIDeleteLdapBindingEvent __example__() {
        APIDeleteLdapBindingEvent event = new APIDeleteLdapBindingEvent();


        return event;
    }

}
