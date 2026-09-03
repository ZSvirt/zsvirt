package org.zstack.twoFactorAuthentication;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.identity.LoginType;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.identity.AccountLoginBackend;

import static org.zstack.core.Platform.argerr;
/**
 * Created by kayo on 2018/7/15.
 *
 * This class is created to keep API compatibility
 *
 * Mean usage are part of AccountLoginBackend, two factory authentication is
 * moved to LoginAuthExtensionPoint
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class TwoFactorAuthenticationAccountBackend extends AccountLoginBackend {
    public static final LoginType loginType = new LoginType(TwoFactorAuthenticationConstant.TWO_FACTOR_ACCOUNT_LOGIN_TYPE);

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public LoginType getLoginType() {
        return loginType;
    }

    @Override
    public boolean authenticate(String name, String password) {
        throw new ApiMessageInterceptionException(argerr("Unsupported AccountType：%s", loginType));
    }
}
