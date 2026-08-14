package org.zstack.kvm;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.host.APIAddHostMsg;
import org.zstack.header.host.HostVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

import java.util.concurrent.TimeUnit;

/**
 * @api
 *
 * add a kvm host
 *
 * @category kvm
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.kvm.APIAddKVMHostMsg": {
"username": "user",
"password": "password",
"port": "port",
"name": "host1",
"managementIp": "localhost",
"clusterUuid": "0f8b6a4702a840bfaf928f04ff0a5da4",
"session": {
"uuid": "0a1b3dd187af40b5871e5ab12b0d7875"
}
}
}
 *
 * @msg
 * {
"org.zstack.kvm.APIAddKVMHostMsg": {
"username": "user",
"password": "password",
"port": "port",
"name": "host1",
"managementIp": "localhost",
"clusterUuid": "0f8b6a4702a840bfaf928f04ff0a5da4",
"session": {
"uuid": "0a1b3dd187af40b5871e5ab12b0d7875"
},
"timeout": 1800000,
"id": "9fca21a7aedb43669a146b0a4c7b9146",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIAddHostEvent`
 */
@TagResourceType(HostVO.class)
@RestRequest(
        path = "/hosts/kvm",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddHostEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APIAddKVMHostMsg extends APIAddHostMsg implements AddKVMHostMessage {
    /**
     * @desc user name used for ssh login.
     * Max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String username;
    /**
     * @desc password for ssh login
     * Max length of 255 characters
     */
    @APIParam(maxLength = 255, password = true)
    @NoLogging
    private String password;


    /**
     * @desc ssh port for login
     * port range (1,65535)
     */
    @APIParam(numberRange = {1, 65535}, required = false)
    private int sshPort = 22;

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }
 
    public static APIAddKVMHostMsg __example__() {
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
        msg.setUsername("userName");
        msg.setPassword("password");
        msg.setSshPort(22);
        msg.setClusterUuid(uuid());
        msg.setName("newHost");
        msg.setManagementIp("127.0.0.1");
        return msg;
    }

}
