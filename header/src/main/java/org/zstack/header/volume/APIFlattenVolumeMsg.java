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
        responseClass = APIFlattenVolumeEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VolumeUuidToVmUuidResolver", field = "uuid", updateOnFailure = true)
public class APIFlattenVolumeMsg extends APIMessage implements VolumeMessage, APIAuditor {
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;

    @APIParam(required = false)
    private boolean dryRun;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        APIFlattenVolumeMsg amsg = (APIFlattenVolumeMsg) msg;
        if (amsg.isDryRun()) {
            return null;
        }

        return new APIAuditor.Result(amsg.getUuid(), VolumeVO.class);
    }

    public static APIFlattenVolumeMsg __example__() {
        APIFlattenVolumeMsg msg = new APIFlattenVolumeMsg();
        msg.setUuid(uuid());
        msg.setDryRun(false);
        return msg;
    }
}
