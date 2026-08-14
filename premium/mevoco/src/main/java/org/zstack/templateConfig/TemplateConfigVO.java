package org.zstack.templateConfig;

import javax.persistence.*;

@Entity
@Table
public class TemplateConfigVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column
    private long id;

    @Column(updatable=false)
    private String name;

    @Column
    private String category;

    @Column(updatable=false)
    private String templateUuid;

    @Column
    private String defaultValue;

    @Column
    private String value;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public TemplateConfigVO toConfig() {
        TemplateConfigVO t = new TemplateConfigVO();
        t.setName(this.getName());
        t.setCategory(this.getCategory());
        t.setTemplateUuid(this.getTemplateUuid());
        t.setDefaultValue(this.getDefaultValue());
        t.setValue(this.getValue());
        return t;
    }

}
