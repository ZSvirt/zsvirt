package org.zstack.compute.vm;

import org.zstack.header.apimediator.ApiMessageInterceptionException;

import static org.zstack.core.Platform.argerr;

public class VmPasswordStrengthConfig {
    private boolean checkPasswordStrength;
    private Integer minimum;
    private Integer maximum;
    private boolean checkUppercase;
    private boolean checkLowercase;
    private boolean checkNumber;
    private boolean checkSpecialWords;

    public boolean isCheckPasswordStrength() {
        return checkPasswordStrength;
    }

    public void setCheckPasswordStrength(boolean checkPasswordStrength) {
        this.checkPasswordStrength = checkPasswordStrength;
    }

    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    public boolean isCheckUppercase() {
        return checkUppercase;
    }

    public void setCheckUppercase(boolean checkUppercase) {
        this.checkUppercase = checkUppercase;
    }

    public boolean isCheckLowercase() {
        return checkLowercase;
    }

    public void setCheckLowercase(boolean checkLowercase) {
        this.checkLowercase = checkLowercase;
    }

    public boolean isCheckNumber() {
        return checkNumber;
    }

    public void setCheckNumber(boolean checkNumber) {
        this.checkNumber = checkNumber;
    }

    public boolean isCheckSpecialWords() {
        return checkSpecialWords;
    }

    public void setCheckSpecialWords(boolean checkSpecialWords) {
        this.checkSpecialWords = checkSpecialWords;
    }

    public void validatePasswordStrengthConfig(String password) {
        if (password.length() > maximum || password.length() < minimum) {
            throw new ApiMessageInterceptionException(argerr("password length must be [%s-%s]", minimum, maximum));
        }

        StringBuffer regex = new StringBuffer("^(?:");
        if (checkLowercase) {
            regex.append("(?=.*[a-z])");
        }
        if (checkUppercase) {
            regex.append("(?=.*[A-Z])");
        }
        if (checkNumber) {
            regex.append("(?=.*[0-9])");
        }
        if (checkSpecialWords) {
            regex.append("(?=.*[^A-Za-z0-9])");
        }
        regex.append(").*$");

        if (!password.matches(regex.toString())) {
            throw new ApiMessageInterceptionException(argerr("password does not match numbers, uppercase and lowercase, and special character combinations"));
        }
    }

    public static void validatePasswordStrengthConfigStr(String passwordStrengthConfigStr) {
        //the format is 01111,6-32 which means [(checkPasswordStrength,lowercase,uppercase,number,specialWords),minimum-maximum], and 0 means false, 1 means true
        String[] configs = passwordStrengthConfigStr.split(",");
        if (configs.length != 2 || configs == null) {
            throw new ApiMessageInterceptionException(argerr("wrong format of password strength config"));
        }

        String characterConfig = configs[0];
        if (!characterConfig.matches("^([0-1]){5}$")) {
            throw new ApiMessageInterceptionException(argerr("wrong format of password strength config"));
        }

        String lengthConfig = configs[1];
        if (!lengthConfig.matches("^([0-9])*-([0-9])*$")) {
            throw new ApiMessageInterceptionException(argerr("wrong format of password strength config"));
        }
        if (Integer.valueOf(lengthConfig.split("-")[0]).compareTo(Integer.valueOf(lengthConfig.split("-")[1])) > 0) {
            throw new ApiMessageInterceptionException(argerr("minimum can not be larger than maximum"));
        }
    }

    public static VmPasswordStrengthConfig toObject(String passwordStrengthConfigStr) {
        //the format is 01111,6-32 which means [(checkPasswordStrength,lowercase,uppercase,number,specialWords),minimum-maximum], and 0 means false, 1 means true
        VmPasswordStrengthConfig vmPasswordStrengthConfig = new VmPasswordStrengthConfig();
        String[] configs = passwordStrengthConfigStr.split(",");

        String characterConfig = configs[0];
        vmPasswordStrengthConfig.setCheckPasswordStrength(characterConfig.substring(0, 1).equals("1"));
        vmPasswordStrengthConfig.setCheckLowercase(characterConfig.substring(1, 2).equals("1"));
        vmPasswordStrengthConfig.setCheckUppercase(characterConfig.substring(2, 3).equals("1"));
        vmPasswordStrengthConfig.setCheckNumber(characterConfig.substring(3, 4).equals("1"));
        vmPasswordStrengthConfig.setCheckSpecialWords(characterConfig.substring(4, 5).equals("1"));

        String lengthConfig = configs[1];
        vmPasswordStrengthConfig.setMinimum(Integer.valueOf(lengthConfig.split("-")[0]));
        vmPasswordStrengthConfig.setMaximum(Integer.valueOf(lengthConfig.split("-")[1]));
        return vmPasswordStrengthConfig;
    }
}
