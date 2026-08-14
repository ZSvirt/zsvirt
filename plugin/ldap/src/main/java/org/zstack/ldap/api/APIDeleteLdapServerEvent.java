package org.zstack.ldap.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteLdapServerEvent extends APIEvent {
    public APIDeleteLdapServerEvent() {
    }

    public APIDeleteLdapServerEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteLdapServerEvent __example__() {
        APIDeleteLdapServerEvent event = new APIDeleteLdapServerEvent();


        return event;
    }

}
