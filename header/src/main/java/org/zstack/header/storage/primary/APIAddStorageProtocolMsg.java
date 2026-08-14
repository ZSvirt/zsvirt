package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.volume.VolumeProtocol;

@RestRequest(
        path = "/primary-storage/protocols",
        responseClass = APIAddStorageProtocolEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
public class APIAddStorageProtocolMsg extends APIMessage implements PrimaryStorageMessage {
    /**
     * @desc primary storage uuid
     */
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;

    @APIParam
    private String outputProtocol;

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getOutputProtocol() {
        return outputProtocol;
    }

    public void setOutputProtocol(String outputProtocol) {
        this.outputProtocol = outputProtocol;
    }

    public static APIAddStorageProtocolMsg __example__() {
        APIAddStorageProtocolMsg msg = new APIAddStorageProtocolMsg();
        msg.setUuid(uuid());
        msg.setOutputProtocol(VolumeProtocol.iSCSI.toString());
        return msg;
    }
}
