package org.zstack.storage.device.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/2
 */
@RestRequest(
        path = "/storage-devices/iscsi/servers/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateIscsiServerEvent.class
)
public class APIUpdateIscsiServerMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = IscsiServerVO.class)
    private String uuid;

    @APIParam(required = false, maxLength = 256, emptyString = false)
    private String name;

    @APIParam(required = false, maxLength = 128)
    private String chapUserName;

    @APIParam(required = false, maxLength = 128)
    @NoLogging
    private String chapUserPassword;

    @APIParam(required = false, validValues = {"Enabled", "Disabled"})
    private String state;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public static APIUpdateIscsiServerMsg __example__() {
        APIUpdateIscsiServerMsg msg = new APIUpdateIscsiServerMsg();
        msg.setUuid(uuid());
        msg.setName("test-iscsi-server");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APIUpdateIscsiServerEvent) rsp).getInventory().getUuid() : "",
                IscsiServerVO.class
        );
    }
}
