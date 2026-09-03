package org.zstack.twoFactorAuthentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.login.LoginManager;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.ldap.LdapManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TwoFactorAuthenticationLdapFactory implements TwoFactorAuthenticationFactory {
    private static final CLogger logger = Utils.getLogger(TwoFactorAuthenticationLdapFactory.class);

    @Autowired
    private LoginManager loginManager;

    @Autowired(required = false)
    private LdapManager ldapManager;

    @Override
    public String getType() {
        return TwoFactorAuthenticationConstant.LOGIN_TYPE_LDAP;
    }

    @Override
    public TwoFactorAuthenticationStruct createAuthentication(TwoFactorAuthenticationParamStruct param) {
        if (ldapManager == null) {
            return null;
        }
        ErrorableValue<AccountThirdPartyAccountSourceRefVO> accountThirdPartyAccountSourceRef = ldapManager.findAccountThirdPartyAccountSourceRefByName(param.getName(), param.getPassword());
        if (!accountThirdPartyAccountSourceRef.isSuccess()) {
            throw new OperationFailureException(accountThirdPartyAccountSourceRef.error);
        }
        if (accountThirdPartyAccountSourceRef.result == null) {
            return null;
        }
        String accountUuid = accountThirdPartyAccountSourceRef.result.getAccountUuid();

        TwoFactorAuthenticationStruct twoFactorAuthenticationStruct = new TwoFactorAuthenticationStruct();
        twoFactorAuthenticationStruct.setAccountUuid(accountUuid);
        return twoFactorAuthenticationStruct;
    }

    @Override
    public String getLoginType() {
        return TwoFactorAuthenticationConstant.LOGIN_TYPE_LDAP;
    }
}