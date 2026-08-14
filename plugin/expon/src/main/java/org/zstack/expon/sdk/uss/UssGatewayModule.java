package org.zstack.expon.sdk.uss;

/** @example
 * {
 * 	"business_network": "172.27.106.107/16",
 * 	"business_port": 9001,
 * 	"core": "0x780",
 * 	"create_time": 1696833401345,
 * 	"domain_name": "172.25.106.113:8089",
 * 	"id": "10074567-580e-4bcf-85a7-2c23b2c32caf",
 * 	"manager_ip": "172.25.106.107",
 * 	"name": "vhost_localhost",
 * 	"protocol": "Vhost",
 * 	"protocol_network": "TCP",
 * 	"server_id": "4705682e-92fb-4e9e-baea-4f30ecad813a",
 * 	"server_name": "node107",
 * 	"server_no": 1,
 * 	"specification": "standard",
 * 	"status": "health",
 * 	"tianshu_id": "0e780e97-c670-4341-960a-6f223baea940",
 * 	"tianshu_name": "tianshu",
 * 	"type": "uss",
 * 	"update_time": 1699987942432,
 * 	"user_type": "uss",
 * 	"uss_id": "11",
 * 	"uss_network": "TCP"
 * }
 */
public class UssGatewayModule {
    private String id;
    private String name;
    private String type;
    private String status;
    private String protocol;
    private String protocolNetwork;
    private String businessNetwork;
    private String ussNetwork;
    private String domainName;
    private String core;
    private String specification;
    private String serverId;
    private String serverName;
    private int serverNo;
    private String managerIp;
    private int businessPort;
    private String tianshuId;
    private String tianshuName;
    private int ussId;
    private String userType;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocolNetwork() {
        return protocolNetwork;
    }

    public void setProtocolNetwork(String protocolNetwork) {
        this.protocolNetwork = protocolNetwork;
    }

    public String getBusinessNetwork() {
        return businessNetwork;
    }

    public void setBusinessNetwork(String businessNetwork) {
        this.businessNetwork = businessNetwork;
    }

    public String getUssNetwork() {
        return ussNetwork;
    }

    public void setUssNetwork(String ussNetwork) {
        this.ussNetwork = ussNetwork;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getCore() {
        return core;
    }

    public void setCore(String core) {
        this.core = core;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getServerNo() {
        return serverNo;
    }

    public void setServerNo(int serverNo) {
        this.serverNo = serverNo;
    }

    public String getManagerIp() {
        return managerIp;
    }

    public void setManagerIp(String managerIp) {
        this.managerIp = managerIp;
    }

    public int getBusinessPort() {
        return businessPort;
    }

    public void setBusinessPort(int businessPort) {
        this.businessPort = businessPort;
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

    public int getUssId() {
        return ussId;
    }

    public void setUssId(int ussId) {
        this.ussId = ussId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
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
