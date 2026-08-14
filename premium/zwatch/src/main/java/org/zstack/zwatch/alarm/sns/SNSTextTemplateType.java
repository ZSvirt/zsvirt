package org.zstack.zwatch.alarm.sns;

/**
 * Created by lining on 2019/8/29.
 */
public enum SNSTextTemplateType {
    ALARM("alarm"),
    EVENT("event"),
    COMBINED("combined");

    private String name;
    SNSTextTemplateType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static SNSTextTemplateType get(String type) {
        for (SNSTextTemplateType tmp: SNSTextTemplateType.values()) {
            if (tmp.toString().equalsIgnoreCase(type)) {
                return tmp;
            }
        }

        throw new IllegalArgumentException(String.format("SNSTextTemplateType[%s] not found" + type));
    }
}
