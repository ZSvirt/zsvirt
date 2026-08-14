package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;

import java.util.concurrent.TimeUnit;

/**
 * @api create a new data volume
 * @category volume
 * @cli
 * @httpMsg {
 * "org.zstack.header.volume.APICreateDataVolumeMsg": {
 * "name": "TestData",
 * "diskOfferingUuid": "9ffa46a4b7214537a582134ef31410f2",
 * "session": {
 * "uuid": "9a944b482a9540f39540f298afeffc2c"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.volume.APICreateDataVolumeMsg": {
 * "name": "TestData",
 * "diskOfferingUuid": "9ffa46a4b7214537a582134ef31410f2",
 * "session": {
 * "uuid": "9a944b482a9540f39540f298afeffc2c"
 * },
 * "timeout": 1800000,
 * "id": "87df9707935f459181d15469b590b03f",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APICreateDataVolumeEvent`
 * @since 0.1.0
 */
@TagResourceType(VolumeVO.class)
@RestRequest(
        path = "/volumes/data",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateDataVolumeEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APICreateDataVolumeMsg extends APICreateMessage implements APIAuditor, VolumeCreateMessage {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     * @optional
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc uuid of disk offering the volume is created from
     */
    @APIParam(required = false, resourceType = DiskOfferingVO.class)
    private String diskOfferingUuid;

    @APIParam(required = false)
    private long diskSize;

    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
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

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public void setDiskOfferingUuid(String diskOfferingUuid) {
        this.diskOfferingUuid = diskOfferingUuid;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public static APICreateDataVolumeMsg __example__() {
        APICreateDataVolumeMsg msg = new APICreateDataVolumeMsg();
        msg.setResourceUuid(uuid());
        msg.setDescription("test-data-volume");
        msg.setDiskOfferingUuid(uuid());
        msg.setPrimaryStorageUuid(uuid());
        msg.setName("test-volume");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateDataVolumeEvent)rsp).getInventory().getUuid() : "", VolumeVO.class);
    }
}
