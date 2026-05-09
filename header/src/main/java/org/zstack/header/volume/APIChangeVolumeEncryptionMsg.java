package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.concurrent.TimeUnit;

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
@RestRequest(
        path = "/volumes/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeVolumeEncryptionEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VolumeUuidToVmUuidResolver", field = "uuid", updateOnFailure = true)
public class APIChangeVolumeEncryptionMsg extends APIMessage implements VolumeMessage, APIAuditor {
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;

    @APIParam
    private boolean encrypted;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        APIChangeVolumeEncryptionMsg amsg = (APIChangeVolumeEncryptionMsg) msg;
        return new APIAuditor.Result(amsg.getUuid(), VolumeVO.class);
    }

    public static APIChangeVolumeEncryptionMsg __example__() {
        APIChangeVolumeEncryptionMsg msg = new APIChangeVolumeEncryptionMsg();
        msg.setUuid(uuid());
        msg.setEncrypted(true);
        return msg;
    }
}
