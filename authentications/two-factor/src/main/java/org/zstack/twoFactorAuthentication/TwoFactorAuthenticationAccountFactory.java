package org.zstack.twoFactorAuthentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.AccountVO_;
import org.zstack.identity.AccountPassword;

import java.util.List;

import static org.zstack.twoFactorAuthentication.TwoFactorAuthenticationConstant.LOGIN_TYPE_ACCOUNT;

public class TwoFactorAuthenticationAccountFactory implements TwoFactorAuthenticationFactory {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public String getType() {
        return LOGIN_TYPE_ACCOUNT;
    }

    @Override
    public TwoFactorAuthenticationStruct createAuthentication(TwoFactorAuthenticationParamStruct param) {
        List<AccountVO> accounts = Q.New(AccountVO.class)
                .eq(AccountVO_.name, param.getName())
                .list();
        accounts.removeIf(account -> !AccountPassword.matchesAccount(param.getPassword(),
                account.getUuid(), account.getPassword()));

        if (accounts.isEmpty()) {
            return null;
        }

        TwoFactorAuthenticationStruct twoFactorAuthenticationStruct = new TwoFactorAuthenticationStruct();
        twoFactorAuthenticationStruct.setAccountUuid(accounts.get(0).getUuid());
        return twoFactorAuthenticationStruct;
    }

    @Override
    public String getLoginType() {
        return AccountConstant.LOGIN_TYPE;
    }
}
