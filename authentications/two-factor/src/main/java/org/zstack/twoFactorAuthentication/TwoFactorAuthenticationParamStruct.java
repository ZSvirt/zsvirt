package org.zstack.twoFactorAuthentication;


public class TwoFactorAuthenticationParamStruct {
    private String name;
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static TwoFactorAuthenticationParamStruct fromApiMessage(APIResetTwoFactorAuthenticationSecretMsg msg) {
        TwoFactorAuthenticationParamStruct param = new TwoFactorAuthenticationParamStruct();
        param.setName(msg.getName());
        param.setPassword(msg.getPassword());
        return param;
    }

    public static TwoFactorAuthenticationParamStruct fromApiMessage(APIGetTwoFactorAuthenticationSecretMsg msg) {
        TwoFactorAuthenticationParamStruct param = new TwoFactorAuthenticationParamStruct();
        param.setName(msg.getName());
        param.setPassword(msg.getPassword());
        return param;
    }
}
