package org.zstack.ldap;

public enum LdapErrors {
    LDAP_ERROR(1000),
    CANNOT_ADD_SAME_LDAP_SERVER(1001),
    UNABLE_TO_GET_SPECIFIED_LDAP_UID(1002),
    TEST_LDAP_CONNECTION_FAILED(1006),
    UNABLE_TO_FIND_LDAP_SERVER(1007),
    LDAP_BINDING_ACCOUNT_ERROR(1008),
    NONE_LDAP_SERVER_ENABLED(1009),
    LDAP_SYNC_ERROR(1010),
    ;

    private String code;

    private LdapErrors(int id) {
        code = String.format("LDAP.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
