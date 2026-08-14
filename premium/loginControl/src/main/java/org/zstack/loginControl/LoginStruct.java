package org.zstack.loginControl;

import java.sql.Timestamp;

/**
 * Created by kayo on 2018/7/10.
 */
public class LoginStruct {
    private String targetResourceIdentity;
    private String captchaUuid;
    private String captchaCode;
    private Timestamp lastUpdatedTime;

    public String getTargetResourceIdentity() {
        return targetResourceIdentity;
    }

    public void setTargetResourceIdentity(String targetResourceIdentity) {
        this.targetResourceIdentity = targetResourceIdentity;
    }

    public String getCaptchaUuid() {
        return captchaUuid;
    }

    public void setCaptchaUuid(String captchaUuid) {
        this.captchaUuid = captchaUuid;
    }

    public Timestamp getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(Timestamp lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
