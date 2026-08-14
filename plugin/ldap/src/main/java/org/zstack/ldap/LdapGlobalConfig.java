package org.zstack.ldap;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by lining on 2017/11/03.
 */
@GlobalConfigDefinition
public class LdapGlobalConfig {
    public static final String CATEGORY = "ldap";

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "member,uniqueMember,memberOf", type = String.class)
    public static GlobalConfig QUERY_LDAP_ENTRY_RETURN_ATTRIBUTES = new GlobalConfig(CATEGORY, "queryLdapEntryReturnAttributes");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = LdapConstant.QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR, type = String.class)
    public static GlobalConfig QUERY_LDAP_ENTRY_RETURN_ATTRIBUTE_SEPARATOR = new GlobalConfig(CATEGORY, "queryLdapEntryReturnAttributeSeparator");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class)
    public static GlobalConfig SKIP_ALL_SSL_CERTS_CHECK = new GlobalConfig(CATEGORY, "skip.all.ssl.certs.check");

    @GlobalConfigValidation(validValues = {"AUTO", "NONE", "PAGE"})
    @GlobalConfigDef(defaultValue = "AUTO", description = "set ldap preferred search mode")
    public static GlobalConfig LDAP_ENTRY_SEARCH_MODE = new GlobalConfig(CATEGORY, "ldap.entry.search.mode");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "NONE", description = "The currently enabled ldap server uuid, or NONE indicates that all ldap servers are currently disabled")
    public static GlobalConfig CURRENT_LDAP_SERVER_UUID = new GlobalConfig(CATEGORY, "current.ldap.server.uuid");

    @GlobalConfigValidation(min = 1)
    @GlobalConfigDef(type = Integer.class, defaultValue = "10000", description = "maximum users sync from ldap server")
    @BindResourceConfig({ThirdPartyAccountSourceVO.class})
    public static GlobalConfig LDAP_MAXIMUM_SYNC_USERS = new GlobalConfig(CATEGORY, "ldap.maximum.sync.users");
}
