package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.io.Serializable;


/**
 * Created by luchukun on 7/29/16.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmConsolePasswordEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APISetVmConsolePasswordMsg extends APIMessage implements VmInstanceMessage, Serializable {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;
    @APIParam
    @NoLogging
    private String consolePassword;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setConsolePassword(String consolePassword) {
        this.consolePassword = consolePassword;
    }

    public String getConsolePassword() {
        return consolePassword;
    }

 
    public static APISetVmConsolePasswordMsg __example__() {
        APISetVmConsolePasswordMsg msg = new APISetVmConsolePasswordMsg();
        msg.uuid = uuid();
        msg.consolePassword = "password";
        return msg;
    }
}
