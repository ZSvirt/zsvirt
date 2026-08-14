package org.zstack.header.storage.primary;

import java.io.Serializable;

/**
 * Created by Administrator on 2017-05-08.
 */
public class CompositePrimaryKeyForPrimaryStorageHostRefVO implements Serializable {
    private String hostUuid;
    private String primaryStorageUuid;

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
        return (hostUuid + primaryStorageUuid).hashCode();
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

        CompositePrimaryKeyForPrimaryStorageHostRefVO other = (CompositePrimaryKeyForPrimaryStorageHostRefVO) obj;
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
