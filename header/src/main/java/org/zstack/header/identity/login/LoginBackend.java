package org.zstack.header.identity.login;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.identity.LoginType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Login implementation interface.
 *
 * Used for support a new kind of login, including authentication, user information and
 * support involve several additional authentication features, like captcha,
 * authentication code and so on.
 */
public interface LoginBackend {
    LoginType getLoginType();

    void login(LoginContext loginContext, ReturnValueCompletion<LoginSessionInfo> completion);

    boolean authenticate(String username, String password);

    String getAccountIdByName(String username);

    void collectUserInfoIntoContext(LoginContext loginContext);

    List<AdditionalAuthFeature> getRequiredAdditionalAuthFeature();

    default Set<String> possibleAccountUuidSetForGettingProcedures(LoginContext loginContext) {
        return Collections.emptySet();
    }
}
