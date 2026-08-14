package org.zstack.sso.cas.message;

import org.zstack.identity.imports.message.CreateThirdPartyAccountSourceMsg;
import org.zstack.sso.cas.header.CasAccountSourceSpec;

public class CreateCasClientMsg extends CreateThirdPartyAccountSourceMsg {
    private CasAccountSourceSpec spec;

    @Override
    public CasAccountSourceSpec getSpec() {
        return spec;
    }

    public void setSpec(CasAccountSourceSpec spec) {
        this.spec = spec;
    }
}
