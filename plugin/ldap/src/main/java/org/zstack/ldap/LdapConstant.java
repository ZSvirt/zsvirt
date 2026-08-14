package org.zstack.ldap;

public interface LdapConstant {
    String SERVICE_ID = "ldap";

    String LDAP_UID_KEY = "cn";

    String LDAP_DN_KEY = "dn";

    String LDAP_OU_KEY = "ou";

    String LDAP_SSL_PREFIX = "ldaps";

    // Empty default
    interface WindowsAD{
        String TYPE = "WindowsAD";
        String MEMBER_KEY = "member";
        String MEMBER_OF_KEY = "memberOf";
        String DN_KEY = "distinguishedName";
        String GLOBAL_UUID_KEY = "objectGUID";
    }

    interface OpenLdap{
        String TYPE = "OpenLdap";
        String MEMBER_KEY = "uniqueMember";
        String MEMBER_OF_KEY = "memberOf";
        String DN_KEY = "entryDN";
        String GLOBAL_UUID_KEY = "entryUUID";
    }

    String[] QUERY_LDAP_ENTRY_MUST_RETURN_ATTRIBUTES = {"cn","name","distinguishedname","displayname","userprincipalname","objectclass","entryDN","distinguishedName"};
    String[] QUERY_WINDOWS_AD_ENTRY_MUST_RETURN_ATTRIBUTES = {"UserAccountControl"};
    String[] QUERY_OPEN_LDAP_ENTRY_MUST_RETURN_ATTRIBUTES = {};

    String QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR = ",";

    String LOGIN_TYPE = "ldap";

    String CURRENT_LDAP_UUID_NONE = "NONE";

    String DEFAULT_PERSON_FILTER = "(objectClass=person)";
}
