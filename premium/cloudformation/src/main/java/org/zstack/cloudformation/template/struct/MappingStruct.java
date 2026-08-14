package org.zstack.cloudformation.template.struct;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mingjian.deng on 2018/5/30.
 */
public class MappingStruct implements ZStackResourceTemplateStruct {
    private String mappingName; // key for mapping
    private Map<String, Object> values = new HashMap<>();

    public String getMappingName() {
        return mappingName;
    }

    public void setMappingName(String mappingName) {
        this.mappingName = mappingName;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
}
