package org.zstack.header.cloudformation;

import org.zstack.header.configuration.PythonClassInventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/7/11.
 */
@PythonClassInventory
public class SupportedResourceStruct implements Serializable {
    private String name;
    private String type;
    private String actionName;
    private List<String> resources = new ArrayList<>();

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

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }
}
