package org.zstack.identity.imports.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.identity.imports.header.ImportAccountSpec;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
public class ImportThirdPartyAccountMsg extends NeedReplyMessage implements AccountSourceMessage {
    private ImportAccountSpec spec;

    public ImportAccountSpec getSpec() {
        return spec;
    }

    public void setSpec(ImportAccountSpec spec) {
        this.spec = spec;
    }

    @Override
    public String getSourceUuid() {
        return spec.getSourceUuid();
    }
}
