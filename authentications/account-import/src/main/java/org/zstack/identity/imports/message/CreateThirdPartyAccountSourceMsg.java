package org.zstack.identity.imports.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;

public abstract class CreateThirdPartyAccountSourceMsg extends NeedReplyMessage {
    public String getType() {
        return getSpec() == null ? null : getSpec().getType();
    }

    public abstract AbstractAccountSourceSpec getSpec();
}
