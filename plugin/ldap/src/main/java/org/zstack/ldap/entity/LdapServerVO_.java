package org.zstack.ldap.entity;

import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(LdapServerVO.class)
public class LdapServerVO_ extends ThirdPartyAccountSourceVO_ {
    public static volatile SingularAttribute<LdapServerVO, String> url;
    public static volatile SingularAttribute<LdapServerVO, String> base;
    public static volatile SingularAttribute<LdapServerVO, String> username;
    public static volatile SingularAttribute<LdapServerVO, String> password;
    public static volatile SingularAttribute<LdapServerVO, String> encryption;
    public static volatile SingularAttribute<LdapServerVO, LdapServerType> serverType;
    public static volatile SingularAttribute<LdapServerVO, String> filter;
    public static volatile SingularAttribute<LdapServerVO, String> usernameProperty;
}
