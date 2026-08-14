package org.zstack.identity;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class IdentityGlobalProperty {
    @GlobalProperty(name = "identity.authorizationBackend.session.cache.size", defaultValue = "500")
    public static int AUTHORIZATION_SESSION_CACHE_SIZE;
}
