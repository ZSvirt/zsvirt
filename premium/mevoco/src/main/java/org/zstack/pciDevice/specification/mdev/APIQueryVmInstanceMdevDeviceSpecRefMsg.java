package org.zstack.pciDevice.specification.mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-05-22.
 */
@AutoQuery(replyClass = APIQueryVmInstanceMdevDeviceSpecRefReply.class, inventoryClass = VmInstanceMdevDeviceSpecRefInventory.class)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/mdev-device-specs",
        optionalPaths = {"/vm-instances/{vmInstanceUuid}/mdev-device-specs/{mdevSpecUuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVmInstanceMdevDeviceSpecRefReply.class
)
public class APIQueryVmInstanceMdevDeviceSpecRefMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("vmInstanceUuid=" + uuid());
    }
}
