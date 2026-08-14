package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.tag.TagResourceType;

import java.util.concurrent.TimeUnit;

@TagResourceType(VmInstanceVO.class)
@RestRequest(
        path = "/vm-instances/metadata/register",
        method = HttpMethod.POST,
        responseClass = APIRegisterVmInstanceFromMetadataEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 1)
public class APIRegisterVmInstanceFromMetadataMsg extends APIMessage {
    @APIParam(nonempty = true, maxLength = 2048)
    private String metadataPath;

    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;

    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(required = false, maxLength = 255)
    private String name;

    public String getMetadataPath() {
        return metadataPath;
    }

    public void setMetadataPath(String metadataPath) {
        this.metadataPath = metadataPath;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static APIRegisterVmInstanceFromMetadataMsg __example__() {
        APIRegisterVmInstanceFromMetadataMsg msg = new APIRegisterVmInstanceFromMetadataMsg();
        String vmUuid = uuid(VmInstanceVO.class);
        msg.metadataPath = String.format("/mnt/ps/vm-metadata/%s.vmmeta", vmUuid);
        msg.primaryStorageUuid = uuid(PrimaryStorageVO.class);
        msg.zoneUuid = uuid(ZoneVO.class);
        msg.clusterUuid = uuid(ClusterVO.class);
        msg.hostUuid = uuid(HostVO.class);
        msg.name = "my-restored-vm";
        return msg;
    }
}
