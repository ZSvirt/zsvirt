package org.zstack.header.cloudformation;

import org.zstack.header.configuration.PythonClassInventory;

import java.io.Serializable;

/**
 * Created by mingjian.deng on 2018/6/20.
 */
@PythonClassInventory
public class StackParameters implements Serializable {
    private String paramName; // key for param
    private String type;
    private String defaultValue;
    private String description;
    private Boolean noEcho;
    private String label;
    private String constraintDescription;
    private String resourceType;

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getNoEcho() {
        return noEcho;
    }

    public void setNoEcho(Boolean noEcho) {
        this.noEcho = noEcho;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getConstraintDescription() {
        return constraintDescription;
    }

    public void setConstraintDescription(String constraintDescription) {
        this.constraintDescription = constraintDescription;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
