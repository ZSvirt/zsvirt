package org.zstack.header.storage.primary;

import org.zstack.header.vo.Index;

import javax.persistence.*;

@MappedSuperclass
public abstract class StorageCapacityAO {
    @Column
    @Index
    protected long totalPhysicalCapacity;

    @Column
    @Index
    protected long availablePhysicalCapacity;

    public long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }

    public long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }

    public abstract String getResourceUuid();
    public abstract String getPrimaryStorageUuid();
}
