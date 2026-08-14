package org.zstack.externalbackup;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/12/4.
 */
@RestRequest(
        path = "/externalbackup",
        optionalPaths = {"/externalbackup/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryExternalBackupReply.class
)
@AutoQuery(replyClass = APIQueryExternalBackupReply.class, inventoryClass = ExternalBackupInventory.class)
public class APIQueryExternalBackupMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}
