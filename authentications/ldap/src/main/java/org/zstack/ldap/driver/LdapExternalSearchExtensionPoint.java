package org.zstack.ldap.driver;

import org.springframework.ldap.core.LdapTemplate;

import javax.naming.directory.SearchControls;
import java.util.function.Predicate;

public interface LdapExternalSearchExtensionPoint {
    LdapSearchedResult trySearch(LdapTemplate ldapTemplate, String filter, SearchControls searchCtls, Predicate<String> resultFilter, Integer count);
}
