package org.zstack.templateConfig;

import javax.persistence.*;

@Entity
@Table
public class GlobalConfigTemplateVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String name;

    @Column
    private String type;

    @Column
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public String getDescription() {return description; }

    public void setDescription(String description) { this.description = description; }


    public GlobalConfigTemplateVO toTemplate() {
        GlobalConfigTemplateVO t = new GlobalConfigTemplateVO();
        t.setUuid(this.getUuid());
        t.setName(this.getName());
        t.setType(this.getType());
        t.setDescription(this.getDescription());
        return t;
    }

}
