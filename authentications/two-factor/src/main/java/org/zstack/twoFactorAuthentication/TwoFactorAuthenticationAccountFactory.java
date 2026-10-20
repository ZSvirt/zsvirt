package org.zstack.twoFactorAuthentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.AccountVO_;

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
        List<String> accountUuidList = Q.New(AccountVO.class)
                .eq(AccountVO_.name, param.getName())
                .eq(AccountVO_.password, param.getPassword())
                .select(AccountVO_.uuid)
                .listValues();

        if (accountUuidList.isEmpty()) {
            return null;
        }

        TwoFactorAuthenticationStruct twoFactorAuthenticationStruct = new TwoFactorAuthenticationStruct();
        twoFactorAuthenticationStruct.setAccountUuid(accountUuidList.get(0));
        return twoFactorAuthenticationStruct;
    }

    @Override
    public String getLoginType() {
        return AccountConstant.LOGIN_TYPE;
    }
}
