package org.zstack.expon.sdk.iscsi;

/**
 * @example
 *{"gateway_ip": "172.26.12.22",
 * "manager_ip": "172.25.12.22",
 * "name": "172-25-12-22",
 * "server_id": "12eb6291-c928-4439-91b5-968c8ebd418a",
 * "tianshu_id": "5e59b7ef-e599-4847-8ab5-f1eb727f2e90",
 * "uss_gw_id": "e06c9f5a-d18f-4531-91f2-b940a98d047c",
 * "uss_name": "iscsi_zstack"}
 */
public class IscsiSeverNode {
    private String gatewayIp;
    private String managerIp;
    private String name;
    private String serverId;
    private String tianshuId;
    private String ussGwId;
    private String ussName;

    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public String getManagerIp() {
        return managerIp;
    }

    public void setManagerIp(String managerIp) {
        this.managerIp = managerIp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getTianshuId() {
        return tianshuId;
    }

    public void setTianshuId(String tianshuId) {
        this.tianshuId = tianshuId;
    }

    public String getUssGwId() {
        return ussGwId;
    }

    public void setUssGwId(String ussGwId) {
        this.ussGwId = ussGwId;
    }

    public String getUssName() {
        return ussName;
    }

    public void setUssName(String ussName) {
        this.ussName = ussName;
    }
}
