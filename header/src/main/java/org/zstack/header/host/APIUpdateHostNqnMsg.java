package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/hosts/nqn/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateHostNqnEvent.class,
        isAction = true
)
public class APIUpdateHostNqnMsg extends APIMessage implements HostMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;
    @APIParam(nonempty = true, emptyString = false)
    private String nqn;

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

    public String getNqn() {
        return nqn;
    }

    public void setNqn(String nqn) {
        this.nqn = nqn;
    }

    public static APIUpdateHostNqnMsg __example__() {
        APIUpdateHostNqnMsg msg = new APIUpdateHostNqnMsg();
        msg.setUuid(uuid());
        msg.setNqn("nqn.2014-08.org.nvmexpress:uuid:748d0363-8366-44db-803b-146effb96988");
        return msg;
    }
}
