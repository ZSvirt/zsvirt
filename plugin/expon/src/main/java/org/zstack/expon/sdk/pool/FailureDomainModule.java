package org.zstack.expon.sdk.pool;


/**
 * @example
 * {"cache_data": false, "cache_meta_data": false, "cache_ratio": 0.0, "cap_change_comment": "", "create_time": 1692933406637, "custom_qos": "",
 * "data_centers": "DC1", "data_size": 1352663040,
 * "db_cache_size": 0, "exclusivity": "", "failure_domain_cache_type": 450887680, "failure_domain_name": "pool", "failure_domain_network": "TCP",
 * "failure_domain_type": "SATA_SSD",
 * "health_status": "health", "health_status_reason": "", "id": "1ad18b89-6e88-4066-b68f-1bf3d2c440ae", "osd_count": 3, "perf_mode": "normal",
 * "physical_pool_type": "local_pool", "pool_id": 1, "pool_uuid": "9a55f8c7-a7fd-46ca-bc10-0c0bdf2370cb",
 * "raw_size": 5761150230528, "real_data_size": 450887680, "recovery_strategy": "low", "redundancy_ploy": "replicated", "replicate_size": 3,
 * "run_status": "normal", "safe_level": "host", "status": "health", "thin_provisioning": 2, "tianshu_id": "0e780e97-c670-4341-960a-6f223baea940",
 * "tianshu_name": "tianshu", "update_time": 1695188453814, "valid_size": 1920383410176}
 */
public class FailureDomainModule {
    private String id;
    private String failureDomainName;

    private long dataSize;
    private long realDataSize;
    private long rawSize;
    private long validSize;

    private String redundancyPloy;
    private int replicateSize;

    private String healthStatus;
    private String healthStatusReason;

    private String perfMode;
    private String customQos;
    private String recoveryStrategy;
    private String runStatus;
    private String failureDomainNetwork;

    private int osdCount;

    private String tianshuId;
    private String tianshuName;

    private long updateTime;
    private long createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFailureDomainName() {
        return failureDomainName;
    }

    public void setFailureDomainName(String failureDomainName) {
        this.failureDomainName = failureDomainName;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public long getRawSize() {
        return rawSize;
    }

    public void setRawSize(long rawSize) {
        this.rawSize = rawSize;
    }

    public long getRealDataSize() {
        return realDataSize;
    }

    public void setRealDataSize(long realDataSize) {
        this.realDataSize = realDataSize;
    }

    public String getRedundancyPloy() {
        return redundancyPloy;
    }

    public void setRedundancyPloy(String redundancyPloy) {
        this.redundancyPloy = redundancyPloy;
    }

    public int getReplicateSize() {
        return replicateSize;
    }

    public void setReplicateSize(int replicateSize) {
        this.replicateSize = replicateSize;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getHealthStatusReason() {
        return healthStatusReason;
    }

    public void setHealthStatusReason(String healthStatusReason) {
        this.healthStatusReason = healthStatusReason;
    }

    public String getPerfMode() {
        return perfMode;
    }

    public void setPerfMode(String perfMode) {
        this.perfMode = perfMode;
    }

    public String getCustomQos() {
        return customQos;
    }

    public void setCustomQos(String customQos) {
        this.customQos = customQos;
    }

    public String getRecoveryStrategy() {
        return recoveryStrategy;
    }

    public void setRecoveryStrategy(String recoveryStrategy) {
        this.recoveryStrategy = recoveryStrategy;
    }

    public String getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(String runStatus) {
        this.runStatus = runStatus;
    }

    public String getFailureDomainNetwork() {
        return failureDomainNetwork;
    }

    public void setFailureDomainNetwork(String failureDomainNetwork) {
        this.failureDomainNetwork = failureDomainNetwork;
    }

    public int getOsdCount() {
        return osdCount;
    }

    public void setOsdCount(int osdCount) {
        this.osdCount = osdCount;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getValidSize() {
        return validSize;
    }

    public void setValidSize(long validSize) {
        this.validSize = validSize;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getAvailableCapacity() {
        return validSize - realDataSize;
    }

    public String getTianshuId() {
        return tianshuId;
    }

    public void setTianshuId(String tianshuId) {
        this.tianshuId = tianshuId;
    }

    public String getTianshuName() {
        return tianshuName;
    }

    public void setTianshuName(String tianshuName) {
        this.tianshuName = tianshuName;
    }
}
