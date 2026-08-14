package org.zstack.pluginpremium.externalapiadapter.exception;

/**
 * Created by Qi Le on 2020/6/4
 */
public class APIAdapterSpecifiedErrorException extends Exception {
    String message;
    String code;

    public APIAdapterSpecifiedErrorException(String code, String message) {
        this.message = message;
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
