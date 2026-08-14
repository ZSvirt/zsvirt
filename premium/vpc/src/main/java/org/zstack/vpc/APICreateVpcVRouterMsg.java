package org.zstack.vpc;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.NewVmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.header.zone.ZoneVO;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by weiwang on 18/09/2017
 */
@TagResourceType(VmInstanceVO.class)
@RestRequest(
        path = "/vpc/virtual-routers",
        method = HttpMethod.POST,
        responseClass = APICreateVpcVRouterEvent.class,
        parameterName = "params"
)
public class APICreateVpcVRouterMsg extends APICreateMessage implements NewVmInstanceMessage, APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(resourceType = VirtualRouterOfferingVO.class)
    private String virtualRouterOfferingUuid;

    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc when not null, vm will be created in the zone this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = ZoneVO.class)
    private String zoneUuid;
    /**
     * @desc when not null, vm will be created in the cluster this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = ClusterVO.class)
    private String clusterUuid;
    /**
     * @desc when not null, vm will be created on the host this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;
    /**
     * @desc when not null, vm will be created on the primary storage this uuid specified
     * @optional
     */
    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuidForRootVolume;

    @APIParam(required = false)
    private List<String> rootVolumeSystemTags;

    @APIParam(required = false)
    private String vmNicParams;

    public String getVirtualRouterOfferingUuid() {
        return virtualRouterOfferingUuid;
    }

    public void setVirtualRouterOfferingUuid(String virtualRouterOfferingUuid) {
        this.virtualRouterOfferingUuid = virtualRouterOfferingUuid;
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

    public static APICreateVpcVRouterMsg __example__() {
        APICreateVpcVRouterMsg msg = new APICreateVpcVRouterMsg();

        msg.setName("TestVPC");
        msg.setDescription("this is a vpc for test");
        msg.setVirtualRouterOfferingUuid(uuid());

        return msg;
    }
    
    public void setZoneUuid(String zoneUuid) { this.zoneUuid = zoneUuid; }

    public String getZoneUuid() { return zoneUuid; }

    public void setClusterUuid(String clusterUuid) { this.clusterUuid = clusterUuid; }

    public String getClusterUuid() { return clusterUuid; }

    public void setHostUuid(String hostUuid) { this.hostUuid = hostUuid; }

    public String getHostUuid() {
        return hostUuid;
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }

    public List<String> getRootVolumeSystemTags() {
        return rootVolumeSystemTags;
    }

    public void setRootVolumeSystemTags(List<String> rootVolumeSystemTags) {
        this.rootVolumeSystemTags = rootVolumeSystemTags;
    }

    @Override
    public List<String> getL3NetworkUuids() {
        return new ArrayList<>();
    }

    public void setVmNicParams(String vmNicParams) {
        this.vmNicParams = vmNicParams;
    }

    @Override
    public String getVmNicParams() {
        return vmNicParams;
    }

    @Override
    public String getDefaultL3NetworkUuid() {
        return null;
    }

    @Override
    public String getType() {
        return ApplianceVmConstant.APPLIANCE_VM_TYPE;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVpcVRouterEvent)rsp).getInventory().getUuid() : "", VpcRouterVmVO.class);
    }
}
