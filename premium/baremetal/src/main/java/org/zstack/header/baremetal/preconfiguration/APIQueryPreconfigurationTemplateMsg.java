package org.zstack.header.baremetal.preconfiguration;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2018-12-26.
 */
@AutoQuery(replyClass = APIQueryPreconfigurationTemplatesReply.class, inventoryClass = PreconfigurationTemplateInventory.class)
@RestRequest(
        path = "/baremetal/preconfigurations",
        optionalPaths = {"/baremetal/preconfigurations/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPreconfigurationTemplatesReply.class
)
public class APIQueryPreconfigurationTemplateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid(PreconfigurationTemplateVO.class));
    }
}
