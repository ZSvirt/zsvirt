package org.zstack.billing.generator;

import java.io.Serializable;

public class CompositePrimaryKeyForBillingResourceLabelVO implements Serializable {
    private String resourceUuid;
    private String labelKey;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    @Override
    public int hashCode() {
        return (resourceUuid + labelKey).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        CompositePrimaryKeyForBillingResourceLabelVO other = (CompositePrimaryKeyForBillingResourceLabelVO) obj;
        if (resourceUuid == null) {
            if (other.resourceUuid != null) {
                return false;
            }
        } else if (!resourceUuid.equals(other.resourceUuid)) {
            return false;
        }
        if (labelKey == null) {
            if (other.labelKey != null) {
                return false;
            }
        } else if (!labelKey.equals(other.labelKey)) {
            return false;
        }

        return true;
    }
}
