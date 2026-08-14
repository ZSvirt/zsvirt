package org.zstack.header.cloudformation;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.cloudformation.template.struct.ActionStruct;

import java.io.Serializable;
import java.util.*;

/**
 * Created by mingjian.deng on 2018/6/6.
 */
@PythonClassInventory
public class PreviewResourceStruct implements Serializable {
    private List<ActionStruct> actions = new ArrayList<>();
    private Map<String, Boolean> conditions = new HashMap<>();

    public List<ActionStruct> getActions() {
        return actions;
    }

    public void setActions(List<ActionStruct> actions) {
        this.actions = actions;
    }

    public Map<String, Boolean> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, Boolean> conditions) {
        this.conditions = conditions;
    }
}
