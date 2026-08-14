package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/hosts/kvm/{hostUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeHostPasswordEvent.class,
        isAction = true
)
public class APIChangeHostPasswordMsg extends APIMessage implements HostMessage {
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(nonempty = true, maxLength = 255)
    @NoLogging
    private String password;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static APIChangeHostPasswordMsg __example__() {
        APIChangeHostPasswordMsg msg = new APIChangeHostPasswordMsg();
        msg.setHostUuid(uuid());
        msg.setPassword("hiddenPassword");
        return msg;
    }
}
