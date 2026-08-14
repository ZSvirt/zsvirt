package org.zstack.identity.imports.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountsSpec;

/**
 * Created by Wenhao.Zhang on 2024/06/03
 */
public class UnbindThirdPartyAccountMsg extends NeedReplyMessage implements AccountSourceMessage {
    private UnbindThirdPartyAccountsSpec spec;

    public UnbindThirdPartyAccountsSpec getSpec() {
        return spec;
    }

    public void setSpec(UnbindThirdPartyAccountsSpec spec) {
        this.spec = spec;
    }

    @Override
    public String getSourceUuid() {
        return spec.getSourceUuid();
    }
}
