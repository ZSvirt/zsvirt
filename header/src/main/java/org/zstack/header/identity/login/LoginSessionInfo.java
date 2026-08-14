package org.zstack.header.identity.login;


public class LoginSessionInfo {
    private String accountUuid;
    private boolean logoutOperatorSession;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public boolean isLogoutOperatorSession() {
        return logoutOperatorSession;
    }

    public void setLogoutOperatorSession(boolean logoutOperatorSession) {
        this.logoutOperatorSession = logoutOperatorSession;
    }
}
