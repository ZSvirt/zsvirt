package org.zstack.header.backup;

import org.apache.logging.log4j.util.Strings;

import static org.zstack.core.Platform.i18n;

public class NonBackupInfo {
    protected static String originValue = i18n("enter the new value here, empty means no change.");

    protected String uuid;
    protected String name;
    protected String attributeName;
    protected String oldValue;
    protected String newValue = i18n("enter the new value here, empty means no change.");
    protected String resourceType;
    protected String resourceDescription;

    public String buildUpdateSql() {
        return Strings.isEmpty(newValue) || originValue.equals(newValue) ? null :
                String.format("update %s set %s = '%s' where uuid = '%s'", resourceType, attributeName, newValue, uuid);
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceDescription() {
        return resourceDescription;
    }

    public void setResourceDescription(String resourceDescription) {
        this.resourceDescription = resourceDescription;
    }

    public String getResourceName() {
        return resourceType.substring(0, 1).toLowerCase() + resourceType.substring(1, resourceType.length() - 2);
    }
}
