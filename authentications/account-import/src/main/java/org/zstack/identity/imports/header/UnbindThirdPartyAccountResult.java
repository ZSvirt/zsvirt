package org.zstack.identity.imports.header;

import org.zstack.header.errorcode.ErrorCode;

public class UnbindThirdPartyAccountResult {
    private String accountUuid;
    private ErrorCode error;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
}
