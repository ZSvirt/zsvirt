package org.zstack.ldap.driver;

import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * Created by miao on 12/19/16.
 */
public class LdapTemplateContextSource {
    private LdapTemplate ldapTemplate;
    private LdapContextSource ldapContextSource;

    public LdapTemplateContextSource(LdapTemplate ldapTemplate, LdapContextSource ldapContextSource) {
        this.ldapTemplate = ldapTemplate;
        this.ldapContextSource = ldapContextSource;
    }


    public LdapTemplate getLdapTemplate() {
        return ldapTemplate;
    }

    public LdapContextSource getLdapContextSource() {
        return ldapContextSource;
    }
}
