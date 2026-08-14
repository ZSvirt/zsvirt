package org.zstack.core.captcha;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by kayo on 2018/7/6.
 */
@RestResponse(fieldsTo = {"all"})
public class APIRefreshCaptchaReply extends APIReply {
    private String captchaUuid;
    private String captcha;

    public String getCaptchaUuid() {
        return captchaUuid;
    }

    public void setCaptchaUuid(String captchaUuid) {
        this.captchaUuid = captchaUuid;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }
}
