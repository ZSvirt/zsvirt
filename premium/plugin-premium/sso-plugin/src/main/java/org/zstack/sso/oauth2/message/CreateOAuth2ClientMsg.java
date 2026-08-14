package org.zstack.sso.oauth2.message;

import org.zstack.identity.imports.message.CreateThirdPartyAccountSourceMsg;
import org.zstack.sso.oauth2.header.OAuth2AccountSourceSpec;

public class CreateOAuth2ClientMsg extends CreateThirdPartyAccountSourceMsg {
    private OAuth2AccountSourceSpec spec;

    @Override
    public OAuth2AccountSourceSpec getSpec() {
        return spec;
    }

    public void setSpec(OAuth2AccountSourceSpec spec) {
        this.spec = spec;
    }
}