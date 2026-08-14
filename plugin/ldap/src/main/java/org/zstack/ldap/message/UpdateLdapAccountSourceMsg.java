package org.zstack.ldap.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.identity.imports.message.AccountSourceMessage;
import org.zstack.ldap.header.LdapAccountSourceSpec;

public class UpdateLdapAccountSourceMsg extends NeedReplyMessage implements AccountSourceMessage {
    private LdapAccountSourceSpec spec;

    public LdapAccountSourceSpec getSpec() {
        return spec;
    }

    public void setSpec(LdapAccountSourceSpec spec) {
        this.spec = spec;
    }

    @Override
    public String getSourceUuid() {
        return spec == null ? null : spec.getUuid();
    }
}