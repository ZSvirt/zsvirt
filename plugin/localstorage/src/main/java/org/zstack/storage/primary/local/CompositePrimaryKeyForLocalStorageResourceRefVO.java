package org.zstack.storage.primary.local;

import java.io.Serializable;

public class CompositePrimaryKeyForLocalStorageResourceRefVO implements Serializable {
    private String resourceUuid;
    private String hostUuid;
    private String primaryStorageUuid;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public int hashCode() {
        return (resourceUuid + hostUuid + primaryStorageUuid).hashCode();
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

        CompositePrimaryKeyForLocalStorageResourceRefVO other = (CompositePrimaryKeyForLocalStorageResourceRefVO) obj;
        if (resourceUuid == null) {
            if (other.resourceUuid != null) {
                return false;
            }
        } else if (!resourceUuid.equals(other.resourceUuid)) {
            return false;
        }
        if (hostUuid == null) {
            if (other.hostUuid != null) {
                return false;
            }
        } else if (!hostUuid.equals(other.hostUuid)) {
            return false;
        }
        if (primaryStorageUuid == null) {
            if (other.primaryStorageUuid != null) {
                return false;
            }
        } else if (!primaryStorageUuid.equals(other.primaryStorageUuid)) {
            return false;
        }

        return true;
    }
}
