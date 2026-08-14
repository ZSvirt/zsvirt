package org.zstack.ldap.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.ldap.entity.LdapServerVO;

@RestRequest(
        path = "/ldap/bindings",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateLdapBindingEvent.class
)
public class APICreateLdapBindingMsg extends APIMessage {
    @APIParam(maxLength = 255)
    private String ldapUid;

    @APIParam(resourceType = AccountVO.class, maxLength = 32)
    private String accountUuid;

    @APIParam(resourceType = LdapServerVO.class, required = false)
    private String ldapServerUuid;

    public String getLdapUid() {
        return ldapUid;
    }

    public void setLdapUid(String ldapUid) {
        this.ldapUid = ldapUid;
    }

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

    public static APICreateLdapBindingMsg __example__() {
        APICreateLdapBindingMsg msg = new APICreateLdapBindingMsg();
        msg.setAccountUuid(uuid());
        msg.setLdapUid("ou=Employee,uid=test");

        return msg;
    }

}
