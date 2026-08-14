package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.header.rest.RestRequest;

/**
 * @api detach a data volume from vm
 * @cli
 * @httpMsg {
 * "org.zstack.header.volume.APIDetachDataVolumeMsg": {
 * "volumeUuid": "57e060d2eb324da4bc65c1c5ad9a6e59",
 * "session": {
 * "uuid": "9f1fddca86544ad282f4c3ffe12d5f10"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.volume.APIDetachDataVolumeMsg": {
 * "volumeUuid": "57e060d2eb324da4bc65c1c5ad9a6e59",
 * "session": {
 * "uuid": "9f1fddca86544ad282f4c3ffe12d5f10"
 * },
 * "timeout": 1800000,
 * "id": "d32847e788d145648cf0e53994d451e2",
 * "serviceId": "api.portal"
 * }
 * }
 * @result See :ref:`APIDetachDataVolumeEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/volumes/{uuid}/vm-instances",
        method = HttpMethod.DELETE,
        responseClass = APIDetachDataVolumeFromVmEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VolumeUuidToVmUuidResolver", field = "uuid")
public class APIDetachDataVolumeFromVmMsg extends APIMessage implements VolumeMessage {
    /**
     * @desc data volume uuid. See :ref:`VolumeInventory`
     */
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;
    @APIParam(required = false, resourceType = VmInstanceVO.class)
    private String vmUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String volumeUuid) {
        this.uuid = volumeUuid;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
 
    public static APIDetachDataVolumeFromVmMsg __example__() {
        APIDetachDataVolumeFromVmMsg msg = new APIDetachDataVolumeFromVmMsg();
        msg.setUuid(uuid());
        msg.setVmUuid(uuid());

        return msg;
    }
}
