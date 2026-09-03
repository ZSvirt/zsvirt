package org.zstack.loginControl.api;

import org.zstack.header.core.captcha.CaptchaVO;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by kayo on 2018/7/10.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetLoginCaptchaReply extends APIReply {
    private String captchaUuid;
    private String captcha = "";

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

    public static APIGetLoginCaptchaReply __example__() {
        APIGetLoginCaptchaReply reply = new APIGetLoginCaptchaReply();

        reply.setCaptchaUuid(uuid(CaptchaVO.class));
        reply.setCaptcha("test");

        return reply;
    }
}
