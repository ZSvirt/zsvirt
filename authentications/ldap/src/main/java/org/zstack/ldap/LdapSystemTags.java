package org.zstack.ldap;

import org.zstack.header.tag.TagDefinition;
import org.zstack.ldap.entity.LdapServerVO;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class LdapSystemTags {
    public static String LDAP_URLS_TOKEN = "ldapUrls";
    public static SystemTag LDAP_URLS = new SystemTag(String.format("ldapUrls::{%s}", LDAP_URLS_TOKEN), LdapServerVO.class);
}
