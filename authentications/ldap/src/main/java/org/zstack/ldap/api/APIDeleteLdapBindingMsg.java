package org.zstack.ldap.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/ldap/bindings/{accountUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteLdapBindingEvent.class
)
public class APIDeleteLdapBindingMsg extends APIMessage {
    @APIParam(resourceType = AccountVO.class)
    private String accountUuid;

    @APINoSee
    private String ldapServerUuid;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getLdapServerUuid() {
        return ldapServerUuid;
    }

    public void setLdapServerUuid(String ldapServerUuid) {
        this.ldapServerUuid = ldapServerUuid;
    }

    public static APIDeleteLdapBindingMsg __example__() {
        APIDeleteLdapBindingMsg msg = new APIDeleteLdapBindingMsg();
        msg.setAccountUuid(uuid());

        return msg;
    }

}
