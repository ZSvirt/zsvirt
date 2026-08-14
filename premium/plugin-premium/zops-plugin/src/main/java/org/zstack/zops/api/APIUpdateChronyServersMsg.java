package org.zstack.zops.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/zops/chrony/actions",
        isAction = true,
        responseClass = APIUpdateChronyServersEvent.class,
        method = HttpMethod.PUT
)
public class APIUpdateChronyServersMsg extends APIMessage {
    @APIParam(required = false, nonempty = true)
    private List<String> internalHostnames;
    @APIParam(required = false, nonempty = true)
    private List<String> externalHostnames;
    public void setInternalHostnames(List<String> internalHostnames) {
        this.internalHostnames = internalHostnames;
    }
    public void setExternalHostnames(List<String> externalHostnames) {
        this.externalHostnames = externalHostnames;
    }
    public List<String> getInternalHostnames() {
        return internalHostnames;
    }

    public List<String> getExternalServers() {
        return externalHostnames;
    }

    public static APIUpdateChronyServersMsg __example__() {
        APIUpdateChronyServersMsg msg = new APIUpdateChronyServersMsg();

        msg.setInternalHostnames(asList("172.0.0.1","172.0.0.2"));
        msg.setExternalHostnames(asList("test1.ntp.com"));
        return msg;
    }

}

