package org.zstack.cloudformation.template.struct;

import org.zstack.header.configuration.PythonClassInventory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by mingjian.deng on 2018/6/6.
 */
@PythonClassInventory
public class ActionStruct implements Cloneable, Serializable {
    // resource name is the defined value in json
    private String resourceName;
    // action name is the real action while creating resource
    private String actionName;
    private int round = 0;
    private Set<String> inDegree = new HashSet<>();
    private Object actions;
    private String error;

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public Set<String> getInDegree() {
        return inDegree;
    }

    public void setInDegree(Set<String> inDegree) {
        this.inDegree = inDegree;
    }

    public Object getActions() {
        return actions;
    }

    public void setActions(Object actions) {
        this.actions = actions;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
