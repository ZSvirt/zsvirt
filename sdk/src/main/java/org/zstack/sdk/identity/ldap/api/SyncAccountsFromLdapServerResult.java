package org.zstack.sdk.identity.ldap.api;

import org.zstack.sdk.identity.imports.header.SyncTaskResult;

public class SyncAccountsFromLdapServerResult {
    public SyncTaskResult result;
    public void setResult(SyncTaskResult result) {
        this.result = result;
    }
    public SyncTaskResult getResult() {
        return this.result;
    }

}
