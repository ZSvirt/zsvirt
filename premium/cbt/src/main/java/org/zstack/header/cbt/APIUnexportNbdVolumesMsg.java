package org.zstack.header.cbt;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.storage.cbt.CbtBackupConstant;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

@Action(category = CbtBackupConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/cbt-task/unexportvolume",
        method = HttpMethod.POST,
        responseClass = APIUnexportNbdVolumesEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APIUnexportNbdVolumesMsg extends APIMessage {
    @APIParam(required = true)
    private List<String> volumeUuids;
    @APIParam(required = true)
    private String vmInstanceUuid;

    public List<String> getVolumeUuids() {
        return volumeUuids;
    }

    public void setVolumeUuids(List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }

    public static APIUnexportNbdVolumesMsg __example__() {
        APIUnexportNbdVolumesMsg msg = new APIUnexportNbdVolumesMsg();
        msg.setVolumeUuids(asList(uuid()));
        msg.setVmInstanceUuid(uuid());
        return msg;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
