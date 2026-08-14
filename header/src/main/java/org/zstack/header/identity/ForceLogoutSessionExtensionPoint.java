package org.zstack.header.identity;

public interface ForceLogoutSessionExtensionPoint {
    void afterForceLogoutSession(IdentityCanonicalEvents.SessionForceLogoutData data);
}
