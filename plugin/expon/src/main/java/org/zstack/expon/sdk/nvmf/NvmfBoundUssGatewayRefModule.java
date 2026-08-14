package org.zstack.expon.sdk.nvmf;

/**
 * {@code @example:{
 * 	"bind_ip": "172.26.12.90",
 * 	"bind_port": 4421,
 * 	"business_network": "172.27.12.90/16",
 * 	"business_port": 9000,
 * 	"core": "0xf0",
 * 	"create_time": 1698735992254,
 * 	"domain_name": "172.27.29.66:8082",
 * 	"id": "8fb252ff-da4d-4cfd-a36b-262d9f742a54",
 * 	"manager_ip": "172.25.12.90",
 * 	"name": "test",
 * 	"nqn": "nqn.2023-10.com.sds.wds:test",
 * 	"nvmf_id": "746c817c-acfa-418f-88d2-d5eb39fc5cab",
 * 	"protocol": "NVMe-oF",
 * 	"protocol_network": "TCP",
 * 	"server_name": "172-25-12-90",
 * 	"server_no": 1,
 * 	"sn": "SDS9a5e66a3",
 * 	"specification": "standard",
 * 	"status": "health",
 * 	"tianshu_id": "e4aba79c-ff0d-4e21-adbc-159f74226377",
 * 	"tianshu_name": "tianshu",
 * 	"update_time": 1698735992254,
 * 	"uss_gw_id": "cae6f136-69b3-4d83-ae64-04cb38e27312",
 * 	"uss_id": "1",
 * 	"uss_name": "nvmf_zstack",
 * 	"vendor": "SDS"
 * }}
 */
public class NvmfBoundUssGatewayRefModule {
    private String id;
    private String name;
    private String protocol;
    private String managerIp;
    private String bindIp;
    private Integer bindPort;
    private String businessNetwork;
    private Integer businessPort;
    private String serverName;
    private String ussGwId;
    private String ussName;
    private String nvmfId;

    public String getNvmfId() {
        return nvmfId;
    }

    public void setNvmfId(String nvmfId) {
        this.nvmfId = nvmfId;
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

    public String getBusinessNetwork() {
        return businessNetwork;
    }

    public void setBusinessNetwork(String businessNetwork) {
        this.businessNetwork = businessNetwork;
    }

    public Integer getBusinessPort() {
        return businessPort;
    }

    public void setBusinessPort(Integer businessPort) {
        this.businessPort = businessPort;
    }

    public String getBindIp() {
        return bindIp;
    }

    public void setBindIp(String bindIp) {
        this.bindIp = bindIp;
    }
}
