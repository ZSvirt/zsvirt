package org.zstack.identity.imports.message;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by Wenhao.Zhang on 2024/06/03
 */
public class DestroyThirdPartyAccountSourceMsg extends NeedReplyMessage implements AccountSourceMessage {
    private String uuid;

    /**
     * "admin" and other special accounts will not be deleted;
     *
     * If account bound to other third-party account source,
     * the account will not be deleted either.
     */
    private boolean deleteAttachedAccounts = true;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isDeleteAttachedAccounts() {
        return deleteAttachedAccounts;
    }

    public void setDeleteAttachedAccounts(boolean deleteAttachedAccounts) {
        this.deleteAttachedAccounts = deleteAttachedAccounts;
    }

    @Override
    public String getSourceUuid() {
        return getUuid();
    }
}
