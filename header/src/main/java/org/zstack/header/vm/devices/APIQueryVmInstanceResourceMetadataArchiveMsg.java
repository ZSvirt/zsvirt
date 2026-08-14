package org.zstack.header.vm.devices;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by LiangHanYu on 2022/6/17 17:31
 */
@AutoQuery(replyClass = APIQueryVmInstanceResourceMetadataArchiveReply.class, inventoryClass = VmInstanceResourceMetadataArchiveInventory.class)
@RestRequest(
        path = "/vmInstance/resource/metadata/archive",
        optionalPaths = {"/vmInstance/resource/metadata/archive/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVmInstanceResourceMetadataArchiveReply.class
)
public class APIQueryVmInstanceResourceMetadataArchiveMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }
}
