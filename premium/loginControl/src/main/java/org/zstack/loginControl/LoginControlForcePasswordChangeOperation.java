package org.zstack.loginControl;

import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.loginControl.entity.LoginAttemptsVO;
import org.zstack.loginControl.entity.LoginAttemptsVO_;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class LoginControlForcePasswordChangeOperation implements LoginControlOperation {
    @Override
    public ErrorCode preLoginCheck(LoginStruct struct) {
        return null;
    }

    @Override
    public void loginSuccess(LoginStruct struct) {
        // if no last update time found, indicate third part login or time is not implement, skip it
        if (struct.getLastUpdatedTime() == null) {
            return;
        }

        if (struct.getLastUpdatedTime().getTime() + TimeUnit.SECONDS.toMillis(PasswordStrategyGlobalConfig.FORCE_CHANGE_PASSWORD_PERIOD.value(Long.class)) < new Date().getTime()) {
            SQL.New(LoginAttemptsVO.class)
                    .set(LoginAttemptsVO_.forceChangePassword, true)
                    .eq(LoginAttemptsVO_.targetResourceIdentity, struct.getTargetResourceIdentity())
                    .update();
        }
    }

    @Override
    public void loginFail(LoginStruct struct, int failureAttempts) {

    }
}
