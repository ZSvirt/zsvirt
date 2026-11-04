package org.zstack.loginControl;

import org.zstack.header.errorcode.ErrorCode;

public interface LoginControlOperation {
    default void prepare(String targetResourceIdentity) {
    }

    ErrorCode preLoginCheck(LoginStruct struct);

    void loginSuccess(LoginStruct struct);

    void loginFail(LoginStruct struct, int failureAttempts);
}
