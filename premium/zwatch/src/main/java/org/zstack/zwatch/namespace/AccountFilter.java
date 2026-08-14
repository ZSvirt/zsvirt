package org.zstack.zwatch.namespace;

import java.util.HashMap;
import java.util.Map;

public class AccountFilter {
    private static Map<String, AccountFilter>  filters = new HashMap<>();

    private String namespace;
    private String labelName;
    private Class resourceType;

    public AccountFilter(String namespace, String labelName, Class resourceType) {
        this.namespace = namespace;
        this.labelName = labelName;
        this.resourceType = resourceType;

        filters.put(namespace, this);
    }

    public static Map<String, AccountFilter> getFilters() {
        return filters;
    }

    public static void setFilters(Map<String, AccountFilter> filters) {
        AccountFilter.filters = filters;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public Class getResourceType() {
        return resourceType;
    }

    public void setResourceType(Class resourceType) {
        this.resourceType = resourceType;
    }
}
