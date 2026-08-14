package org.zstack.templateConfig;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = GlobalConfigTemplateVO.class)
public class GlobalConfigTemplateInventory {
    private String uuid;
    private String name;
    private String type;
    private String description;

    public static GlobalConfigTemplateInventory valueOf(GlobalConfigTemplateVO vo) {
        GlobalConfigTemplateInventory inv = new GlobalConfigTemplateInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setType(vo.getType());
        inv.setDescription(vo.getDescription());
        return inv;
    }

    public static List<GlobalConfigTemplateInventory> valueOf(Collection<GlobalConfigTemplateVO> vos) {
        List<GlobalConfigTemplateInventory> invs = new ArrayList<GlobalConfigTemplateInventory>();
        for (GlobalConfigTemplateVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public static GlobalConfigTemplateInventory valueOf(GlobalConfigTemplate t) {
        GlobalConfigTemplateInventory inv = new GlobalConfigTemplateInventory();
        inv.setUuid(t.getUuid());
        inv.setName(t.getName());
        inv.setType(t.getType());
        inv.setDescription(t.getDescription());
        return inv;
    }
    
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
