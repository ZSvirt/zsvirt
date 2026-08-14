package org.zstack.kvm;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.APIUpdateHostEvent;
import org.zstack.header.host.APIUpdateHostMsg;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/15/2015.
 */
@RestRequest(
        path = "/hosts/kvm/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateHostEvent.class,
        isAction = true
)
public class APIUpdateKVMHostMsg extends APIUpdateHostMsg {
    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String username;
    @APIParam(maxLength = 255, required = false, password = true)
    @NoLogging
    private String password;
    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer sshPort;

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static APIUpdateKVMHostMsg __example__() {
        APIUpdateKVMHostMsg msg = new APIUpdateKVMHostMsg();
        msg.setUuid(uuid());
        msg.setUsername("userName");
        msg.setPassword("password");
        msg.setSshPort(22);

        return msg;
    }

}
