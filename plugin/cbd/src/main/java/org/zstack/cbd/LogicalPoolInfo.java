package org.zstack.cbd;

/**
 * @author Xingwei Yu
 * @date 2025/1/7 13:17
 */
public class LogicalPoolInfo {
    private long physicalPoolID;
    private RedundanceAndPlaceMentPolicy redundanceAndPlaceMentPolicy;
    private long logicalPoolID;
    private long usedSize;
    private long quota;
    private long createTime;
    private int type;
    private long rawWalUsedSize;
    private int allocateStatus;
    private long rawUsedSize;
    private String physicalPoolName;
    private long capacity;
    private String logicalPoolName;
    private String userPolicy;
    private long allocatedSize;

    public long getPhysicalPoolID() {
        return physicalPoolID;
    }

    public void setPhysicalPoolID(long physicalPoolID) {
        this.physicalPoolID = physicalPoolID;
    }

    public RedundanceAndPlaceMentPolicy getRedundanceAndPlaceMentPolicy() {
        return redundanceAndPlaceMentPolicy;
    }

    public void setRedundanceAndPlaceMentPolicy(RedundanceAndPlaceMentPolicy redundanceAndPlaceMentPolicy) {
        this.redundanceAndPlaceMentPolicy = redundanceAndPlaceMentPolicy;
    }

    public long getLogicalPoolID() {
        return logicalPoolID;
    }

    public void setLogicalPoolID(long logicalPoolID) {
        this.logicalPoolID = logicalPoolID;
    }

    public long getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(long usedSize) {
        this.usedSize = usedSize;
    }

    public long getQuota() {
        return quota;
    }

    public void setQuota(long quota) {
        this.quota = quota;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getRawWalUsedSize() {
        return rawWalUsedSize;
    }

    public void setRawWalUsedSize(long rawWalUsedSize) {
        this.rawWalUsedSize = rawWalUsedSize;
    }

    public int getAllocateStatus() {
        return allocateStatus;
    }

    public void setAllocateStatus(int allocateStatus) {
        this.allocateStatus = allocateStatus;
    }

    public long getRawUsedSize() {
        return rawUsedSize;
    }

    public void setRawUsedSize(long rawUsedSize) {
        this.rawUsedSize = rawUsedSize;
    }

    public String getPhysicalPoolName() {
        return physicalPoolName;
    }

    public void setPhysicalPoolName(String physicalPoolName) {
        this.physicalPoolName = physicalPoolName;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public String getLogicalPoolName() {
        return logicalPoolName;
    }

    public void setLogicalPoolName(String logicalPoolName) {
        this.logicalPoolName = logicalPoolName;
    }

    public String getUserPolicy() {
        return userPolicy;
    }

    public void setUserPolicy(String userPolicy) {
        this.userPolicy = userPolicy;
    }

    public long getAllocatedSize() {
        return allocatedSize;
    }

    public void setAllocatedSize(long allocatedSize) {
        this.allocatedSize = allocatedSize;
    }

    public static class RedundanceAndPlaceMentPolicy {
        private int copysetNum;
        private int replicaNum;
        private int zoneNum;

        public int getCopysetNum() {
            return copysetNum;
        }

        public void setCopysetNum(int copysetNum) {
            this.copysetNum = copysetNum;
        }

        public int getReplicaNum() {
            return replicaNum;
        }

        public void setReplicaNum(int replicaNum) {
            this.replicaNum = replicaNum;
        }

        public int getZoneNum() {
            return zoneNum;
        }

        public void setZoneNum(int zoneNum) {
            this.zoneNum = zoneNum;
        }
    }
}
