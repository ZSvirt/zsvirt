package org.zstack.loginControl;

public class PasswordStrengthConfig {
    private boolean enabled;
    private Integer minimum;
    private Integer maximum;
    private boolean checkUppercase;
    private boolean checkLowercase;
    private boolean checkNumber;
    private boolean checkSpecialWords;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
}
