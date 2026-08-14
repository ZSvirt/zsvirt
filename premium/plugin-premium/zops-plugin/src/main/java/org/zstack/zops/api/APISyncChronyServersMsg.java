package org.zstack.zops.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zops/chrony/actions",
        isAction = true,
        responseClass = APISyncChronyServersEvent.class,
        method = HttpMethod.PUT
)
public class APISyncChronyServersMsg extends APIMessage {
    public static APISyncChronyServersMsg __example__() {
        return new APISyncChronyServersMsg();
    }
}
