package org.zstack.header.sshkeypair;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQuerySshKeyPairReply.class, inventoryClass = SshKeyPairInventory.class)
@RestRequest(
        path = "/ssh-key-pair",
        optionalPaths = {"/ssh-key-pair/{uuid}"},
        responseClass = APIQuerySshKeyPairReply.class,
        method = HttpMethod.GET
)
public class APIQuerySshKeyPairMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}
