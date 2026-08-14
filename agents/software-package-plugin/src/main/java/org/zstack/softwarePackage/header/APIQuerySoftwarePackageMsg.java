package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQuerySoftwarePackageReply.class, inventoryClass = SoftwarePackageInventory.class)
@RestRequest(
        path = "/software-package",
        optionalPaths = {"/software-package/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySoftwarePackageReply.class
)
public class APIQuerySoftwarePackageMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid(SoftwarePackageVO.class));
    }
}
