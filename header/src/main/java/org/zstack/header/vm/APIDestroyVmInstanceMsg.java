package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @api destroy a vm instance
 * @cli
 * @httpMsg {
 * "org.zstack.header.vm.APIDestroyVmInstanceMsg": {
 * "uuid": "a3047245c27c49a08943f2dd37e65a63",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "616b27b659dd496a9a98cd62348bfef0"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.vm.APIDestroyVmInstanceMsg": {
 * "uuid": "a3047245c27c49a08943f2dd37e65a63",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "616b27b659dd496a9a98cd62348bfef0"
 * },
 * "timeout": 1800000,
 * "id": "cced165a85fe4fcbb81d55b27695a6ea",
 * "serviceId": "api.portal"
 * }
 * }
 * @result
 * @since 0.1.0
 */
@RestRequest(
        path = "/vm-instances/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDestroyVmInstanceEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APIDestroyVmInstanceMsg extends APIDeleteMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(getUuid());
    }

    public static APIDestroyVmInstanceMsg __example__() {
        APIDestroyVmInstanceMsg msg = new APIDestroyVmInstanceMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
