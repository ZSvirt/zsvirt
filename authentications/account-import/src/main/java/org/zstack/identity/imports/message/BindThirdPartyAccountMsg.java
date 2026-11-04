package org.zstack.identity.imports.message;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by Wenhao.Zhang on 2024/06/03
 */
public class BindThirdPartyAccountMsg extends NeedReplyMessage implements AccountSourceMessage {
    private String accountUuid;
    private String sourceUuid;
    private String credentials;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @Override
    public String getSourceUuid() {
        return sourceUuid;
    }

    public void setSourceUuid(String sourceUuid) {
        this.sourceUuid = sourceUuid;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }
}
