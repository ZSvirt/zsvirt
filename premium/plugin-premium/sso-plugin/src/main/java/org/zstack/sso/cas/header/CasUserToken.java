package org.zstack.sso.cas.header;

import java.util.HashMap;
import java.util.Map;

public class CasUserToken {
    public String clientUuid;

    public String usernameProperty;
    /**
     * In CAS, username is {@link org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO#getCredentials()}
     */
    public String username;

    public Map<String, String> attributes = new HashMap<>();
}
