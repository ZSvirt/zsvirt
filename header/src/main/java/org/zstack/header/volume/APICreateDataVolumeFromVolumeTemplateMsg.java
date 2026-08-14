package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;

import java.util.concurrent.TimeUnit;

/**
 */
@TagResourceType(VolumeVO.class)
@RestRequest(
        path = "/volumes/data/from/data-volume-templates/{imageUuid}",
        responseClass = APICreateDataVolumeFromVolumeTemplateEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APICreateDataVolumeFromVolumeTemplateMsg extends APICreateMessage implements APIAuditor, VolumeCreateMessage {
    @APIParam(resourceType = ImageVO.class)
    private String imageUuid;
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;
    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDiskOfferingUuid() {
        return null;
    }

    @Override
    public void setDiskOfferingUuid(String diskOfferingUuid) {

    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public long getDiskSize() {
        return 0;
    }

    @Override
    public void setDiskSize(long diskSize) {

    }

    public static APICreateDataVolumeFromVolumeTemplateMsg __example__() {
        APICreateDataVolumeFromVolumeTemplateMsg msg = new APICreateDataVolumeFromVolumeTemplateMsg();
        msg.setDescription("dataVolume-from-volume-template");
        msg.setName("data-volume-1");
        msg.setPrimaryStorageUuid(uuid());
        msg.setHostUuid(uuid());
        msg.setImageUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateDataVolumeFromVolumeTemplateEvent)rsp).getInventory().getUuid() : "", VolumeVO.class);
    }
}
