package org.zstack.resourceconfig;

import java.util.List;

public class ResourceConfigStruct {
    private String value;
    private List<ResourceConfigInventory> effectiveConfigs;
    private String name;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<ResourceConfigInventory> getEffectiveConfigs() {
        return effectiveConfigs;
    }

    public void setEffectiveConfigs(List<ResourceConfigInventory> effectiveConfigs) {
        this.effectiveConfigs = effectiveConfigs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
