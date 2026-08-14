package org.zstack.header.baremetal.instance;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 7/4/18.
 */
@RestRequest(
        path = "/baremetal/instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateBaremetalInstanceEvent.class
)
public class APIUpdateBaremetalInstanceMsg extends APIMessage implements BaremetalInstanceMessage {
    @APIParam(resourceType = BaremetalInstanceVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 255, required = false, password = true)
    @NoLogging
    private String password;

    @APIParam(validValues = {"Linux"}, required = false)
    private String platform;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @Override
    public String getBaremetalInstanceUuid() {
        return getUuid();
    }

    public static APIUpdateBaremetalInstanceMsg __example__() {
        APIUpdateBaremetalInstanceMsg msg = new APIUpdateBaremetalInstanceMsg();
        msg.uuid = uuid();
        msg.name = "BM-1-RENAMED";
        return msg;
    }
}
