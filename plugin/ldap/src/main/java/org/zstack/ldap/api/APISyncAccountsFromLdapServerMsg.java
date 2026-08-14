package org.zstack.ldap.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.ldap.entity.LdapServerVO;

/**
 * Created by Wenhao.Zhang on 2024/06/04
 */
@RestRequest(
        path = "/ldap/servers/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APISyncAccountsFromLdapServerEvent.class
)
public class APISyncAccountsFromLdapServerMsg extends APIMessage {
    @APIParam(resourceType = LdapServerVO.class)
    private String uuid;
    @APIParam(validEnums = {SyncCreatedAccountStrategy.class}, required = false)
    private String createAccountStrategy;
    @APIParam(validEnums = {SyncDeletedAccountStrategy.class}, required = false)
    private String deleteAccountStrategy;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCreateAccountStrategy() {
        return createAccountStrategy;
    }

    public void setCreateAccountStrategy(String createAccountStrategy) {
        this.createAccountStrategy = createAccountStrategy;
    }

    public String getDeleteAccountStrategy() {
        return deleteAccountStrategy;
    }

    public void setDeleteAccountStrategy(String deleteAccountStrategy) {
        this.deleteAccountStrategy = deleteAccountStrategy;
    }

    public static APISyncAccountsFromLdapServerMsg __example__() {
        APISyncAccountsFromLdapServerMsg msg = new APISyncAccountsFromLdapServerMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
