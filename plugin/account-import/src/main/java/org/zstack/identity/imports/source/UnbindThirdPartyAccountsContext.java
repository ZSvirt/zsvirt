package org.zstack.identity.imports.source;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountResult;

public class UnbindThirdPartyAccountsContext {
    public ErrorCode errorForAccountExecution;
    public String accountUuid;
    public String sourceUuid;

    public UnbindThirdPartyAccountResult makeResult() {
        UnbindThirdPartyAccountResult result = new UnbindThirdPartyAccountResult();
        result.setAccountUuid(accountUuid);
        result.setError(errorForAccountExecution);
        return result;
    }
}
