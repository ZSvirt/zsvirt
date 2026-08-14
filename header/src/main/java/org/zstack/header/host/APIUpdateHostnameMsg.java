package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/hosts/hostname/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateHostnameEvent.class,
        isAction = true
)
public class APIUpdateHostnameMsg extends APIMessage implements HostMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;
    @APIParam(nonempty = true, emptyString = false)
    private String hostname;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public static APIUpdateHostnameMsg __example__() {
        APIUpdateHostnameMsg msg = new APIUpdateHostnameMsg();
        msg.setUuid(uuid());
        msg.setHostname("user");
        return msg;
    }
}
