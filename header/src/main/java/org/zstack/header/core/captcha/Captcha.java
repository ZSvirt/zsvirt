package org.zstack.header.core.captcha;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * Created by kayo on 2018/7/5.
 */
public interface Captcha {
    CaptchaVO generateCaptcha(String targetResourceIdentity);

    void refreshCaptcha(String uuid, ReturnValueCompletion<CaptchaStruct> completion);

    CaptchaVO refreshCaptcha(String uuid);

    boolean verifyCaptcha(String uuid, String verifyCode, String targetResourceIdentify);
}
