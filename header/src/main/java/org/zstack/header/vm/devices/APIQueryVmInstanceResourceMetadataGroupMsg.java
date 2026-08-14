package org.zstack.header.vm.devices;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by LiangHanYu on 2022/6/20 18:03
 */
@AutoQuery(replyClass = APIQueryVmInstanceResourceMetadataGroupReply.class, inventoryClass = VmInstanceResourceMetadataGroupInventory.class)
@RestRequest(
        path = "/vmInstance/resource/metadata/group",
        optionalPaths = {"/vmInstance/resource/metadata/group/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVmInstanceResourceMetadataGroupReply.class
)
public class APIQueryVmInstanceResourceMetadataGroupMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }
}
