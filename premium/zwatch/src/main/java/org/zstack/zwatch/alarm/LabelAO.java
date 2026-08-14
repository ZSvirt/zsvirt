package org.zstack.zwatch.alarm;

import org.zstack.zwatch.datatype.Label;

import javax.persistence.*;

@MappedSuperclass
public class LabelAO {
    @Id
    @Column
    private String uuid;
    @Column(name="`key`")
    private String key;
    @Column
    @Enumerated(EnumType.STRING)
    private Label.Operator operator;
    @Column
    private String value;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Label.Operator getOperator() {
        return operator;
    }

    public void setOperator(Label.Operator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
