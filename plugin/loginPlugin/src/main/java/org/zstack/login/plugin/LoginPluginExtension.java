package org.zstack.login.plugin;

public interface LoginPluginExtension {
    String getLoginPluginName();

    LoginUserInfo login(String username, String password);
}
