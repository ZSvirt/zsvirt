package org.zstack.expon.sdk.cluster;

/**
 * @example
 * {
 *  "cluster_mode": "normal",
 *  "create_time": 1658073607684,
 *  "domain_name": "sdfo.sgo.dfog",
 *  "health_status": "health",
 *  "id": "b64ec569-ec33-445e-b62c-59c8ebbda702",
 *  "name": "tianshu",
 *  "node": 3,
 *  "run_status": "complete",
 *  "tikv_name": "isdfofdg",
 *  "update_time": 1658073697076
 *  }
 */
public class TianshuClusterModule {
    private String id;
    private String name;
    private String domainName;
    private String tikvName;
    private String clusterMode;
    private String healthStatus;
    private String runStatus;
    private int node;
    private long createTime;
    private long updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getTikvName() {
        return tikvName;
    }

    public void setTikvName(String tikvName) {
        this.tikvName = tikvName;
    }

    public String getClusterMode() {
        return clusterMode;
    }

    public void setClusterMode(String clusterMode) {
        this.clusterMode = clusterMode;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(String runStatus) {
        this.runStatus = runStatus;
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
