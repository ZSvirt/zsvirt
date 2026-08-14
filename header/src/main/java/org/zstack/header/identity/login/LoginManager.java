package org.zstack.header.identity.login;

public interface LoginManager {
    String SERVICE_ID = "login";

    LoginBackend getLoginBackend(String loginType);

    String getUserIdByName(String username, String loginType);
}
