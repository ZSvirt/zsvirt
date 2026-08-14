package org.zstack.header.core.captcha;

/**
 * Created by kayo on 2018/7/5.
 */
public class CaptchaStruct {
    private String uuid;
    private String captcha;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }
}
