package org.zstack.sdk;

import org.zstack.sdk.ResourceType;

public class ResourceStruct  {

    public java.lang.String resourceName;
    public void setResourceName(java.lang.String resourceName) {
        this.resourceName = resourceName;
    }
    public java.lang.String getResourceName() {
        return this.resourceName;
    }

    public java.lang.String resourceType;
    public void setResourceType(java.lang.String resourceType) {
        this.resourceType = resourceType;
    }
    public java.lang.String getResourceType() {
        return this.resourceType;
    }

    public java.lang.String deletePolicy;
    public void setDeletePolicy(java.lang.String deletePolicy) {
        this.deletePolicy = deletePolicy;
    }
    public java.lang.String getDeletePolicy() {
        return this.deletePolicy;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.util.Set inDegree;
    public void setInDegree(java.util.Set inDegree) {
        this.inDegree = inDegree;
    }
    public java.util.Set getInDegree() {
        return this.inDegree;
    }

    public java.lang.String action;
    public void setAction(java.lang.String action) {
        this.action = action;
    }
    public java.lang.String getAction() {
        return this.action;
    }

    public java.util.Map properties;
    public void setProperties(java.util.Map properties) {
        this.properties = properties;
    }
    public java.util.Map getProperties() {
        return this.properties;
    }

    public java.lang.Object results;
    public void setResults(java.lang.Object results) {
        this.results = results;
    }
    public java.lang.Object getResults() {
        return this.results;
    }

    public ResourceType type;
    public void setType(ResourceType type) {
        this.type = type;
    }
    public ResourceType getType() {
        return this.type;
    }

    public boolean created;
    public void setCreated(boolean created) {
        this.created = created;
    }
    public boolean getCreated() {
        return this.created;
    }

    public boolean mockFailed;
    public void setMockFailed(boolean mockFailed) {
        this.mockFailed = mockFailed;
    }
    public boolean getMockFailed() {
        return this.mockFailed;
    }

}
