package org.zstack.header.resourceattribute.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Map;

@PythonClassInventory
public class ResourceAttributeConstraintParam {
    public long id;
    public String type;
    public String parameter;

    public static ResourceAttributeConstraintParam valueOf(Map<String, Object> map) {
        ResourceAttributeConstraintParam constraint = new ResourceAttributeConstraintParam();
        if (map.get("id") instanceof Number) {
            constraint.id = ((Number) map.get("id")).longValue();
        }
        constraint.type = (String) map.get("type");
        constraint.parameter = (String) map.get("parameter");
        return constraint;
    }

    public static ResourceAttributeConstraintParam valueOf(ResourceAttributeConstraintVO vo) {
        ResourceAttributeConstraintParam constraint = new ResourceAttributeConstraintParam();
        constraint.id = vo.getId();
        constraint.type = vo.getType();
        constraint.parameter = vo.getParameter();
        return constraint;
    }

    @Override
    public String toString() {
        return JSONObjectUtil.toJsonString(this);
    }
}
