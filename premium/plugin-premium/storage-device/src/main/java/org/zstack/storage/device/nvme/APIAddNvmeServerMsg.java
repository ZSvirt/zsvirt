package org.zstack.storage.device.nvme;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/storage-devices/nvme/servers",
        method = HttpMethod.POST,
        responseClass = APIAddNvmeServerEvent.class,
        parameterName = "params"
)
public class APIAddNvmeServerMsg extends APICreateMessage implements APIAuditor {
    @APIParam(required = false, emptyString = false, maxLength = 256)
    private String name;

    @APIParam(maxLength = 64)
    private String ip;

    @APIParam(required = false, numberRange = {1, 65535})
    private Integer port = 4420;

    @APIParam(maxLength = 32, validValues = {"tcp", "rdma"})
    private String transport;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public static APIAddNvmeServerMsg __example__() {
        APIAddNvmeServerMsg msg = new APIAddNvmeServerMsg();
        msg.setIp("10.0.0.201");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APIAddNvmeServerEvent) rsp).getInventory().getUuid() : "",
                NvmeServerVO.class
        );
    }
}
