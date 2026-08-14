package org.zstack.templateConfig;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = TemplateConfigVO.class)
public class TemplateConfigInventory {
    private String templateUuid;
    private String category;
    private String name;
    private String defaultValue;
    private String value;

    public static TemplateConfigInventory valueOf(TemplateConfigVO vo) {
        TemplateConfigInventory inv = new TemplateConfigInventory();
        inv.setTemplateUuid(vo.getTemplateUuid());
        inv.setName(vo.getName());
        inv.setDefaultValue(vo.getDefaultValue());
        inv.setCategory(vo.getCategory());
        inv.setValue(vo.getValue());
        return inv;
    }

    public static List<TemplateConfigInventory> valueOf(Collection<TemplateConfigVO> vos) {
        List<TemplateConfigInventory> invs = new ArrayList<TemplateConfigInventory>();
        for (TemplateConfigVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public static TemplateConfigInventory valueOf(TemplateConfig c) {
        TemplateConfigInventory inv = new TemplateConfigInventory();
        inv.setTemplateUuid(c.getTemplateUuid());
        inv.setName(c.getName());
        inv.setDefaultValue(c.getDefaultValue());
        inv.setCategory(c.getCategory());
        inv.setValue(c.getValue());
        return inv;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
