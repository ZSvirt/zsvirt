package org.zstack.ldap;

import org.zstack.core.Platform;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.ldap.driver.LdapUtil;
import org.zstack.ldap.entity.LdapServerVO;

/**
 * Created by miao on 16-9-6.
 */
public interface LdapManager {
    boolean isValid(String uid, String password);
    ErrorableValue<AccountThirdPartyAccountSourceRefVO> findAccountThirdPartyAccountSourceRefByName(String ldapLoginName, String ldapLoginPassword);
    ErrorableValue<String> findCurrentLdapServerUuid();
    ErrorableValue<LdapServerVO> findCurrentLdapServer();

    default LdapUtil createDriver() {
        return Platform.New(LdapUtil::new);
    }
}
