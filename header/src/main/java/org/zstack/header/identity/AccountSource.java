package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClass;

import javax.annotation.Nullable;

/**
 * Where an account was originally created. Immutable after creation (ZSV-12257).
 */
@PythonClass
public enum AccountSource {
    Local,
    OpenLdap,
    WindowsAD,
    CAS,
    OAuth2;

    public static AccountSource fromLdapServerTypeName(@Nullable String serverType) {
        if (OpenLdap.name().equals(serverType)) {
            return OpenLdap;
        }
        return WindowsAD;
    }
}
