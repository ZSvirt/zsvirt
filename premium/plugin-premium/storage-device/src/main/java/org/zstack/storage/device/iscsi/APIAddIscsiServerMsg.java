package org.zstack.storage.device.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/2
 */
@RestRequest(
        path = "/storage-devices/iscsi/servers",
        method = HttpMethod.POST,
        responseClass = APIAddIscsiServerEvent.class,
        parameterName = "params"
)
public class APIAddIscsiServerMsg extends APICreateMessage implements APIAuditor {
    @APIParam(required = false, emptyString = false, maxLength = 256)
    private String name;

    @APIParam(maxLength = 64)
    private String ip;

    @APIParam(required = false, numberRange = {1, 65535})
    private Integer port = 3260;

    @APIParam(required = false, maxLength = 128)
    private String chapUserName;

    @APIParam(required = false, maxLength = 128)
    @NoLogging
    private String chapUserPassword;

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

    public String getChapUserName() {
        return chapUserName;
    }

    public void setChapUserName(String chapUserName) {
        this.chapUserName = chapUserName;
    }

    public String getChapUserPassword() {
        return chapUserPassword;
    }

    public void setChapUserPassword(String chapUserPassword) {
        this.chapUserPassword = chapUserPassword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static APIAddIscsiServerMsg __example__() {
        APIAddIscsiServerMsg msg = new APIAddIscsiServerMsg();
        msg.setIp("10.0.0.201");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APIAddIscsiServerEvent) rsp).getInventory().getUuid() : "",
                IscsiServerVO.class
        );
    }
}
