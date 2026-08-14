package org.zstack.header.identity.login;

public interface APICaptchaMessage {
    String getCaptchaUuid();

    String getVerifyCode();
}
