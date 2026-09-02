package org.zstack.ldap.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.identity.imports.header.SyncTaskResult;

/**
 * Created by Wenhao.Zhang on 2024/06/04
 */
@RestResponse(fieldsTo = "result")
public class APISyncAccountsFromLdapServerEvent extends APIEvent {
    private SyncTaskResult result;

    public APISyncAccountsFromLdapServerEvent(String apiId) {
        super(apiId);
    }

    public APISyncAccountsFromLdapServerEvent() {
        super(null);
    }

    public SyncTaskResult getResult() {
        return result;
    }

    public void setResult(SyncTaskResult result) {
        this.result = result;
    }
}
