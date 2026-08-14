package org.zstack.header.allocator;

/**
 */
public class ReservedHostCapacity {
    private long reservedCpuCapacity;
    private long reservedMemoryCapacity;

    public long getReservedCpuCapacity() {
        return reservedCpuCapacity;
    }

    public void setReservedCpuCapacity(long reservedCpuCapacity) {
        this.reservedCpuCapacity = reservedCpuCapacity;
    }

    public long getReservedMemoryCapacity() {
        return reservedMemoryCapacity;
    }

    public void setReservedMemoryCapacity(long reservedMemoryCapacity) {
        this.reservedMemoryCapacity = reservedMemoryCapacity;
    }
}
