package org.zstack.ovf.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.ovf.datatype.ImagePackageInventory;

import java.util.Collections;
import java.util.List;

/**
 * Created by Qi Le on 2022/4/26
 */
@AutoQuery(replyClass = APIQueryImagePackageReply.class, inventoryClass = ImagePackageInventory.class)
@RestRequest(
        path = "/image-packages",
        optionalPaths = {"/image-packages/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryImagePackageReply.class
)
public class APIQueryImagePackageMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}
