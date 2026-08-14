package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * @api api event for message :ref:`APIAttachVolumeToVmEvent`
 * @cli
 * @httpMsg {
 * "org.zstack.header.volume.APIAttachVolumeToVmMsg": {
 * "vmInstanceUuid": "e979b10eb753412e8588d26b4b544fdc",
 * "volumeUuid": "ad36d6fcdb1d4bbb9d2fa4b0be993fdc",
 * "session": {
 * "uuid": "49c7e4c1fc18499a9477dd426436a8a4"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.volume.APIAttachVolumeToVmMsg": {
 * "vmInstanceUuid": "e979b10eb753412e8588d26b4b544fdc",
 * "volumeUuid": "ad36d6fcdb1d4bbb9d2fa4b0be993fdc",
 * "session": {
 * "uuid": "49c7e4c1fc18499a9477dd426436a8a4"
 * },
 * "timeout": 1800000,
 * "id": "87176dac994d42b4989b8479339033e2",
 * "serviceId": "api.portal"
 * }
 * }
 * @result See :ref:`APIAttachVolumeToVmEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/volumes/{volumeUuid}/vm-instances/{vmInstanceUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachDataVolumeToVmEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIAttachDataVolumeToVmMsg extends APIMessage implements VolumeMessage {
    /**
     * @desc vm uuid. see :ref:`VmInstanceInventory`
     */
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;
    /**
     * @desc data volume uuid.
     */
    @APIParam(resourceType = VolumeVO.class)
    private String volumeUuid;

    public String getVmUuid() {
        return vmInstanceUuid;
    }

    public void setVmUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }


    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVmInstanceUuid() {
        return getVmUuid();
    }
 
    public static APIAttachDataVolumeToVmMsg __example__() {
        APIAttachDataVolumeToVmMsg msg = new APIAttachDataVolumeToVmMsg();
        msg.setVolumeUuid(uuid());
        msg.setVmUuid(uuid());

        return msg;
    }
}
