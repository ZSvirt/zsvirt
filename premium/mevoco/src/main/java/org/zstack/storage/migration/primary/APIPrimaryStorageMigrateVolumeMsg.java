package org.zstack.storage.migration.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.storage.migration.StorageMigrationMessage;

import java.util.concurrent.TimeUnit;

/**
 * Created by GuoYi on 8/30/17.
 */
@RestRequest(
        path = "/primary-storage/volumes/{volumeUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIPrimaryStorageMigrateVolumeEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VolumeUuidToVmUuidResolver", field = "volumeUuid", updateOnFailure = true)
public class APIPrimaryStorageMigrateVolumeMsg extends APIMessage implements StorageMigrationMessage, APIAuditor {
    @APIParam(resourceType = VolumeVO.class)
    private String volumeUuid;

    @APINoSee
    private String srcPrimaryStorageUuid;

    @APIParam(resourceType = PrimaryStorageVO.class)
    private String dstPrimaryStorageUuid;

    @APINoSee
    private String type;

    @APINoSee
    private String vmInstanceUuid;

    public static APIPrimaryStorageMigrateVolumeMsg __example__() {
        APIPrimaryStorageMigrateVolumeMsg msg = new APIPrimaryStorageMigrateVolumeMsg();
        msg.setVolumeUuid(uuid());
        msg.setDstPrimaryStorageUuid(uuid());
        return msg;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getSrcPrimaryStorageUuid() {
        return srcPrimaryStorageUuid;
    }

    public void setSrcPrimaryStorageUuid(String srcPrimaryStorageUuid) {
        this.srcPrimaryStorageUuid = srcPrimaryStorageUuid;
    }

    public String getDstPrimaryStorageUuid() {
        return dstPrimaryStorageUuid;
    }

    public void setDstPrimaryStorageUuid(String dstPrimaryStorageUuid) {
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess())  {
            return null;
        }

        APIPrimaryStorageMigrateVolumeEvent evt = (APIPrimaryStorageMigrateVolumeEvent) rsp;
        Result r = new APIAuditor.Result(evt.getInventory().getUuid(), VolumeVO.class);
        return r;
    }
}
