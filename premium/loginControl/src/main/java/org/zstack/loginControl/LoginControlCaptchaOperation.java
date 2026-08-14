package org.zstack.loginControl;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.captcha.Captcha;
import org.zstack.header.core.captcha.CaptchaVO;
import org.zstack.header.core.captcha.CaptchaVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.loginControl.entity.LoginAttemptsVO;
import org.zstack.loginControl.entity.LoginAttemptsVO_;

import static org.zstack.core.Platform.err;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LoginControlCaptchaOperation implements LoginControlOperation {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private Captcha captcha;

    @Override
    public void prepare(String targetResourceIdentity) {
        CaptchaVO vo = new CaptchaVO();
        vo.setUuid(Platform.getUuid());
        vo.setVerifyCode("");
        vo.setCaptcha("");
        vo.setTargetResourceIdentity(targetResourceIdentity);
        dbf.persist(vo);
    }

    @Override
    public ErrorCode preLoginCheck(LoginStruct struct) {
        if (!LoginControlGlobalConfig.LOGIN_CONTROL.value(Boolean.class)) {
            return null;
        }

        int attempts = Q.New(LoginAttemptsVO.class)
                .select(LoginAttemptsVO_.attempts)
                .eq(LoginAttemptsVO_.targetResourceIdentity, struct.getTargetResourceIdentity())
                .findValue();

        if (attempts >= LoginControlGlobalConfig.LOGIN_ATTEMPTS_MAXIMUM.value(Long.class)) {
            String verifyCode = struct.getCaptchaCode();
            if (struct.getCaptchaUuid() == null || verifyCode == null) {
                return err(LoginControlErrors.CAPTCHA_MISSING, "Missing verify code. Get verify code through captcha related API: GetLoginCaptcha");
            }

            if (!captcha.verifyCaptcha(struct.getCaptchaUuid(), verifyCode, struct.getTargetResourceIdentity())) {
                return err(LoginControlErrors.CAPTCHA_ERROR, "Wrong verify code");
            }
        }

        return null;
    }

    @Override
    public void loginSuccess(LoginStruct struct) {
        if (!LoginControlGlobalConfig.LOGIN_CONTROL.value(Boolean.class)) {
            return;
        }

        SQL.New(CaptchaVO.class).eq(CaptchaVO_.targetResourceIdentity, struct.getTargetResourceIdentity()).hardDelete();
    }

    @Override
    public void loginFail(LoginStruct struct, int failureAttempts) {
        if (!LoginControlGlobalConfig.LOGIN_CONTROL.value(Boolean.class)) {
            return;
        }

        if (failureAttempts < LoginControlGlobalConfig.LOGIN_ATTEMPTS_MAXIMUM.value(Long.class)) {
            return;
        }

        if (struct.getCaptchaUuid() == null) {
            return;
        }

        captcha.refreshCaptcha(struct.getCaptchaUuid());
    }
}
