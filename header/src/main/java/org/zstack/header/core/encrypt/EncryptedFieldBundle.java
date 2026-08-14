package org.zstack.header.core.encrypt;

/**
 * @author hanyu.liang
 * @date 2023/5/5 16:35
 */
public class EncryptedFieldBundle {
    private String encryptedType;
    private String encryptedClass;
    private String encryptedColumn;
    private String conditionValue;
    private String conditionKey;

    public String getEncryptedType() {
        return encryptedType;
    }

    public void setEncryptedType(String encryptedType) {
        this.encryptedType = encryptedType;
    }

    public String getEncryptedClass() {
        return encryptedClass;
    }

    public void setEncryptedClass(String encryptedClass) {
        this.encryptedClass = encryptedClass;
    }

    public String getEncryptedColumn() {
        return encryptedColumn;
    }

    public void setEncryptedColumn(String encryptedColumn) {
        this.encryptedColumn = encryptedColumn;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public String getConditionKey() {
        return conditionKey;
    }

    public void setConditionKey(String conditionKey) {
        this.conditionKey = conditionKey;
    }
}
