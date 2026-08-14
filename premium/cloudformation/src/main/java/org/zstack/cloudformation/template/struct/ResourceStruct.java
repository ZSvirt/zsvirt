package org.zstack.cloudformation.template.struct;

import org.zstack.header.configuration.PythonClassInventory;

import java.util.*;

/**
 * Created by mingjian.deng on 2018/5/30.
 */
@PythonClassInventory
public class ResourceStruct implements ZStackResourceTemplateStruct, Cloneable {
    private String resourceName; // key for resource
    private String resourceType;
    private String deletePolicy;
    private String description;
    private Set<String> inDegree = new HashSet<>();
    private String action;
    private Map<String, Object> properties = new HashMap<>();
    private Object results;
    private ResourceType type;
    private boolean created = false;
    private boolean mockFailed = false;

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Set<String> getInDegree() {
        return inDegree;
    }

    public void setInDegree(Set<String> inDegree) {
        this.inDegree = inDegree;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getDeletePolicy() {
        return deletePolicy;
    }

    public void setDeletePolicy(String deletePolicy) {
        this.deletePolicy = deletePolicy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public Object getResults() {
        return results;
    }

    public void setResults(Object results) {
        this.results = results;
    }

    public boolean isMockFailed() {
        return mockFailed;
    }

    public void setMockFailed(boolean mockFailed) {
        this.mockFailed = mockFailed;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public ResourceStruct clone() {
        ResourceStruct d = new ResourceStruct();
        d.setType(type);
        d.setCreated(created);
        d.setProperties(properties);
        d.setResults(results);
        d.setDescription(description);
        Iterator<String> it = inDegree.iterator();
        while (it.hasNext()) {
            d.getInDegree().add(it.next());
        }
        d.setDeletePolicy(deletePolicy);
        d.setAction(action);
        d.setResourceName(resourceName);
        d.setResourceType(resourceType);
        d.setMockFailed(mockFailed);

        return d;
    }
}
