package org.zstack.storage.primary.local;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeVO;

import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 11/16/2015.
 */
@RestRequest(
        path = "/primary-storage/local-storage/volumes/{volumeUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APILocalStorageMigrateVolumeEvent.class,
        isAction = true
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VolumeUuidToVmUuidResolver", field = "volumeUuid", updateOnFailure = true)
public class APILocalStorageMigrateVolumeMsg extends APIMessage implements PrimaryStorageMessage, APIAuditor {
    @APIParam(resourceType = VolumeVO.class)
    private String volumeUuid;
    @APIParam(resourceType = HostVO.class)
    private String destHostUuid;
    @APINoSee
    private String primaryStorageUuid;
    @APINoSee
    private String vmInstanceUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APILocalStorageMigrateVolumeMsg __example__() {
        APILocalStorageMigrateVolumeMsg msg = new APILocalStorageMigrateVolumeMsg();

        msg.setVolumeUuid(uuid());
        msg.setDestHostUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APILocalStorageMigrateVolumeMsg)msg).primaryStorageUuid, PrimaryStorageVO.class);
    }
}
