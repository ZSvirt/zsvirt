package org.zstack.ldap;

import org.zstack.header.tag.TagDefinition;
import org.zstack.ldap.entity.LdapServerVO;
import org.zstack.tag.PatternedSystemTag;

/**
 */
@TagDefinition
public class LdapSystemTags {
    public static String LDAP_URLS_TOKEN = "ldapUrls";
    public static PatternedSystemTag LDAP_URLS = new PatternedSystemTag(String.format("ldapUrls::{%s}", LDAP_URLS_TOKEN), LdapServerVO.class);
}
