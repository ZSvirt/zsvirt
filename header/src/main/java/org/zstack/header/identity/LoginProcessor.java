package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;

import java.sql.Timestamp;

/**
 * Created by kayo on 2018/7/9.
 */
public interface LoginProcessor {
    class Result {
        private String verifyCode;
        private String captchaUuid;
        private String targetResourceIdentity;
        private Timestamp lastUpdatedTime;

        public String getVerifyCode() {
            return verifyCode;
        }

        public void setVerifyCode(String verifyCode) {
            this.verifyCode = verifyCode;
        }

        public String getCaptchaUuid() {
            return captchaUuid;
        }

        public void setCaptchaUuid(String captchaUuid) {
            this.captchaUuid = captchaUuid;
        }

        public String getTargetResourceIdentity() {
            return targetResourceIdentity;
        }

        public void setTargetResourceIdentity(String targetResourceIdentity) {
            this.targetResourceIdentity = targetResourceIdentity;
        }

        public Timestamp getLastUpdatedTime() {
            return lastUpdatedTime;
        }

        public void setLastUpdatedTime(Timestamp lastUpdatedTime) {
            this.lastUpdatedTime = lastUpdatedTime;
        }
    }

    LoginType getLoginType();

    Class getMessageClass();

    String resourceChecker(String resourceName);

    Result getMessageParams(APIMessage message);

    boolean authenticate(String name, String password);
}
