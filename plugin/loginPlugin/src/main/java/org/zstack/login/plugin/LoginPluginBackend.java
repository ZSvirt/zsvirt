package org.zstack.login.plugin;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.LoginType;
import org.zstack.header.identity.login.AdditionalAuthFeature;
import org.zstack.header.identity.login.LoginBackend;
import org.zstack.header.identity.login.LoginContext;
import org.zstack.header.identity.login.LoginSessionInfo;
import org.zstack.utils.BeanUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class LoginPluginBackend implements LoginBackend {
    LoginType loginType = new LoginType("plugin");

    private static final Map<String, LoginPluginExtension> loginPluginExtensionMap = new HashMap<>();

    static {
        BeanUtils.reflections.getSubTypesOf(LoginPluginExtension.class).forEach(clz -> {
            try {
                LoginPluginExtension ext = clz.getConstructor().newInstance();
                LoginPluginExtension old = loginPluginExtensionMap.get(ext.getLoginPluginName());
                if (old != null) {
                    throw new CloudRuntimeException(String.format("duplicate LoginPluginExtension[%s, %s] with type[%s]",
                            clz, old, ext.getLoginPluginName()));
                }

                String type = ext.getLoginPluginName();
                if (type == null) {
                    return;
                }

                loginPluginExtensionMap.put(ext.getLoginPluginName(), ext);
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });
    }

    @Override
    public LoginType getLoginType() {
        return loginType;
    }

    @Override
    public void login(LoginContext loginContext, ReturnValueCompletion<LoginSessionInfo> completion) {
        if (loginContext.getLoginPluginName() == null) {
            throw new OperationFailureException(operr("missing loginPluginName"));
        }

        LoginPluginExtension ext = loginPluginExtensionMap.get(loginContext.getLoginPluginName());
        if (ext == null) {
            throw new OperationFailureException(operr("no login plugin named %s", loginContext.getLoginPluginName()));
        }

        LoginUserInfo info = ext.login(loginContext.getUsername(), loginContext.getPassword());
        if (info == null || info.getUsername() == null) {
            completion.fail(operr("missing LoginUserInfo when use plugin login", loginContext.getLoginPluginName()));
            return;
        }

        LoginSessionInfo session = new LoginSessionInfo();
        completion.success(session);
    }

    @Override
    public boolean authenticate(String username, String password) {
        return true;
    }

    @Override
    public String getUserIdByName(String username) {
        return null;
    }

    @Override
    public void collectUserInfoIntoContext(LoginContext loginContext) {
        // plugin should not modify inner data
    }

    @Override
    public List<AdditionalAuthFeature> getRequiredAdditionalAuthFeature() {
        return null;
    }
}
