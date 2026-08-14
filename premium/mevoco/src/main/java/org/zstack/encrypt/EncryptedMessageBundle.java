package org.zstack.encrypt;

import org.zstack.header.core.encrypt.EncryptionParamAllowed;
import org.zstack.header.message.APIMessage;

/**
 * Created by Wenhao.Zhang on 22/11/21
 */
public class EncryptedMessageBundle {
    private String encryptionType;
    private String cipherText;
    private APIMessage apiMessage;
    private EncryptionParamAllowed annotation;

    private EncryptionParamParser parser;
    private Object userInfo;

    public String getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(String encryptionType) {
        this.encryptionType = encryptionType;
    }

    public String getCipherText() {
        return cipherText;
    }

    public void setCipherText(String cipherText) {
        this.cipherText = cipherText;
    }

    public APIMessage getApiMessage() {
        return apiMessage;
    }

    public void setApiMessage(APIMessage apiMessage) {
        this.apiMessage = apiMessage;
    }

    public EncryptionParamAllowed getAnnotation() {
        return annotation;
    }

    public void setAnnotation(EncryptionParamAllowed annotation) {
        this.annotation = annotation;
    }

    public EncryptionParamParser getParser() {
        return parser;
    }

    public void setParser(EncryptionParamParser parser) {
        this.parser = parser;
    }

    public Object getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(Object userInfo) {
        this.userInfo = userInfo;
    }
}
