package org.zstack.expon.sdk.iscsi;

/**
 * {@code @example:{
 *  "create_time": 1658053009572,
 *  "gateway_ip": "10.1.28.108",
 *  "health_status": "health",
 *  "health_status_reason": "",
 *  "iscsi_gw_id": "db63d92f-8e00-444a-9d52-6a8fa5ae6f3f",
 *  "manager_ip": "10.1.22.108",
 *  "name": "node108",
 *  "node_id": "17c35f03-40e4-40e9-b92a-000045df0570",
 *  "pg_tag": 14,
 *  "port": 3260,
 *  "server_id": "7a471b9a-96df-47f2-bdcd-0799c956de13",
 *  "update_time": 1658055193723,
 *  "uss_gw_id": "80ba4d41-95df-464e-b8f8-2d1df37eeeb1",
 *  "uss_id": "17"
 *  }}
 */
public class IscsiBoundUssGatewayRefModule {
    private String name;
    private String managerIp;
    private String gatewayIp;
    private int port;
    private String serverId;
    private String nodeId;
    private String iscsiGwId;
    private String healthStatus;
    private String healthStatusReason;
    private int pgTag;
    private String ussGwId;
    private String ussId;
    private long createTime;
    private long updateTime;

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatusReason(String healthStatusReason) {
        this.healthStatusReason = healthStatusReason;
    }

    public String getHealthStatusReason() {
        return healthStatusReason;
    }

    public void setIscsiGwId(String iscsiGwId) {
        this.iscsiGwId = iscsiGwId;
    }

    public String getIscsiGwId() {
        return iscsiGwId;
    }

    public void setManagerIp(String managerIp) {
        this.managerIp = managerIp;
    }

    public String getManagerIp() {
        return managerIp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
