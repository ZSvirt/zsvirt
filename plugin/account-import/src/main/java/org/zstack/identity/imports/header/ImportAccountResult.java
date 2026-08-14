package org.zstack.identity.imports.header;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefInventory;

public class ImportAccountResult {
    private AccountThirdPartyAccountSourceRefInventory ref;
    private ErrorCode error;

    public AccountThirdPartyAccountSourceRefInventory getRef() {
        return ref;
    }

    public void setRef(AccountThirdPartyAccountSourceRefInventory ref) {
        this.ref = ref;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
}
