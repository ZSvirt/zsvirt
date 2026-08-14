package org.zstack.loginControl;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.loginControl.entity.LoginAttemptsVO;
import org.zstack.loginControl.entity.LoginAttemptsVO_;

import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;

public class LoginControlLockIdentityOperation implements LoginControlOperation {
    @Override
    public ErrorCode preLoginCheck(LoginStruct struct) {
        if (!PasswordStrategyGlobalConfig.ENABLE_LOCK_LOGIN_ATTEMPTS_MAXIMUM.value(Boolean.class)) {
            return null;
        }

        if (isLocked(struct.getTargetResourceIdentity())) {
            Timestamp timestamp = Q.New(LoginAttemptsVO.class)
                    .select(LoginAttemptsVO_.unlockDate)
                    .eq(LoginAttemptsVO_.targetResourceIdentity, struct.getTargetResourceIdentity())
                    .findValue();

            // if login target is lock, throw exception
            return err(LoginControlErrors.IDENTITY_LOCKED_ERROR, "this user is locked, remaining time: %s seconds", TimeUnit.MILLISECONDS.toSeconds(timestamp.getTime() - System.currentTimeMillis()));
        }

        return null;
    }

    @Override
    public void loginSuccess(LoginStruct struct) {

    }

    @Override
    public void loginFail(LoginStruct struct, int failureAttempts) {
        lockTargetIfNeeded(failureAttempts, struct.getTargetResourceIdentity());
    }

    private boolean lockTargetIfNeeded(long attempts, String targetResourceIdentity) {
        if (!PasswordStrategyGlobalConfig.ENABLE_LOCK_LOGIN_ATTEMPTS_MAXIMUM.value(Boolean.class)) {
            return false;
        }

        if (attempts < PasswordStrategyGlobalConfig.LOCK_LOGIN_ATTEMPTS_MAXIMUM.value(Long.class)) {
            return false;
        }

        // already locked
        if (Q.New(LoginAttemptsVO.class)
                .eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity)
                .eq(LoginAttemptsVO_.locked, true).isExists()) {
            return true;
        }

        SQL.New(LoginAttemptsVO.class)
                .eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity)
                .set(LoginAttemptsVO_.locked, true)
                .set(LoginAttemptsVO_.unlockDate, new Timestamp(new Date().getTime() + TimeUnit.SECONDS.toMillis(PasswordStrategyGlobalConfig.LOCK_LOGIN_PERIOD.value(Long.class))))
                .update();
        return true;
    }

    private boolean isLocked(String targetResourceIdentity) {
        // if locked but time reached unlock time, update locked as false
        if (Q.New(LoginAttemptsVO.class)
                .eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity)
                .eq(LoginAttemptsVO_.locked, true)
                .lt(LoginAttemptsVO_.unlockDate, new Timestamp(new Date().getTime()))
                .isExists()) {
            SQL.New(LoginAttemptsVO.class)
                    .eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity)
                    .set(LoginAttemptsVO_.attempts, 0)
                    .set(LoginAttemptsVO_.locked, false)
                    .update();

            return false;
        } else {
            return Q.New(LoginAttemptsVO.class)
                    .eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity)
                    .eq(LoginAttemptsVO_.locked, true).isExists();
        }
    }
}
