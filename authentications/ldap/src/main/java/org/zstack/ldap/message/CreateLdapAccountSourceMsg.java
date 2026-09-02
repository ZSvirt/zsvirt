package org.zstack.ldap.message;

import org.zstack.identity.imports.message.CreateThirdPartyAccountSourceMsg;
import org.zstack.ldap.header.LdapAccountSourceSpec;

public class CreateLdapAccountSourceMsg extends CreateThirdPartyAccountSourceMsg {
    private LdapAccountSourceSpec spec;

    @Override
    public LdapAccountSourceSpec getSpec() {
        return spec;
    }

    public void setSpec(LdapAccountSourceSpec spec) {
        this.spec = spec;
    }
}