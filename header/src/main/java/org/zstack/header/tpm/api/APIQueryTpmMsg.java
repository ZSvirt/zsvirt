package org.zstack.header.tpm.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tpm.entity.TpmInventory;
import org.zstack.header.tpm.entity.TpmVO;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryTpmReply.class, inventoryClass = TpmInventory.class)
@RestRequest(
        path = "/tpms",
        optionalPaths = {"/tpms/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryTpmReply.class
)
public class APIQueryTpmMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + DocUtils.createFixedUuid(TpmVO.class));
    }
}
