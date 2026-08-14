package org.zstack.identity.imports.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;

/**
 * AccountThirdPartySyncMsg will call {@link ImportThirdPartyAccountMsg}
 *
 * Created by Wenhao.Zhang on 2024/06/05
 */
public class SyncThirdPartyAccountMsg extends NeedReplyMessage implements AccountSourceMessage {
    private String sourceUuid;
    private SyncCreatedAccountStrategy createAccountStrategy;
    private SyncDeletedAccountStrategy deleteAccountStrategy;

    @Override
    public String getSourceUuid() {
        return sourceUuid;
    }

    public void setSourceUuid(String sourceUuid) {
        this.sourceUuid = sourceUuid;
    }

    public SyncCreatedAccountStrategy getCreateAccountStrategy() {
        return createAccountStrategy;
    }

    public void setCreateAccountStrategy(SyncCreatedAccountStrategy createAccountStrategy) {
        this.createAccountStrategy = createAccountStrategy;
    }

    public SyncDeletedAccountStrategy getDeleteAccountStrategy() {
        return deleteAccountStrategy;
    }

    public void setDeleteAccountStrategy(SyncDeletedAccountStrategy deleteAccountStrategy) {
        this.deleteAccountStrategy = deleteAccountStrategy;
    }
}
