package org.zstack.autoscaling.template;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.zone.ZoneVO;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Create by weiwang at 2018/8/16
 */
@TagResourceType(AutoScalingVmTemplateVO.class)
@RestRequest(
        path = "/autoscaling/vmtemplate",
        method = HttpMethod.POST,
        responseClass = APICreateAutoScalingTemplateEvent.class,
        parameterName = "params"
)
public class APICreateAutoScalingVmTemplateMsg extends APICreateAutoScalingTemplateMsg {
    @APIParam(maxLength = 255)
    private String vmInstanceName;

    @APIParam(required = false, maxLength = 2048)
    private String vmInstanceDescription;

    @APIParam(resourceType = InstanceOfferingVO.class)
    private String vmInstanceOfferingUuid;

    @APIParam(resourceType = ImageVO.class)
    private String imageUuid;

    @APIParam(resourceType = L3NetworkVO.class, nonempty = true)
    private List<String> l3NetworkUuids;

    @APIParam(validValues = {"UserVm", "ApplianceVm"}, required = false)
    private String vmInstanceType = "UserVm";

    @APIParam(required = false, resourceType = DiskOfferingVO.class)
    private String rootDiskOfferingUuid;

    @APIParam(required = false, resourceType = DiskOfferingVO.class)
    private List<String> dataDiskOfferingUuids;

    @APIParam(required = false, resourceType = ZoneVO.class)
    private String vmInstanceZoneUuid;

    @APIParam(required = false, resourceType = ClusterVO.class)
    private String vmInstanceClusterUuid;

    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuidForRootVolume;

    @APIParam
    private String defaultL3NetworkUuid;

    @APIParam(required = false, validValues = {"InstantStart", "CreateStopped"})
    private String strategy = "InstantStart";

    public static APICreateAutoScalingVmTemplateMsg __example__() {
        APICreateAutoScalingVmTemplateMsg msg = new APICreateAutoScalingVmTemplateMsg();

        String defaultL3Uuid = uuid();

        msg.setName("template1");
        msg.setVmInstanceName("vm1");
        msg.setDescription("for new vm");
        msg.setVmInstanceDescription("vm1 desc");
        msg.setVmInstanceClusterUuid(uuid());
        msg.setDataDiskOfferingUuids(asList(defaultL3Uuid, uuid()));
        msg.setImageUuid(uuid());
        msg.setVmInstanceOfferingUuid(uuid());
        msg.setL3NetworkUuids(asList(uuid()));
        msg.setVmInstanceType("UserVm");
        msg.setRootDiskOfferingUuid(uuid());
        msg.setDataDiskOfferingUuids(Arrays.asList(uuid()));
        msg.setVmInstanceZoneUuid(uuid());
        msg.setVmInstanceClusterUuid(uuid());
        msg.setHostUuid(uuid());
        msg.setPrimaryStorageUuidForRootVolume(uuid());
        msg.setDefaultL3NetworkUuid(defaultL3Uuid);
        msg.setStrategy("InstantStart");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateAutoScalingTemplateEvent)rsp).getInventory().getUuid() : "", AutoScalingVmTemplateVO.class);
    }

    public String getVmInstanceOfferingUuid() {
        return vmInstanceOfferingUuid;
    }

    public void setVmInstanceOfferingUuid(String vmInstanceOfferingUuid) {
        this.vmInstanceOfferingUuid = vmInstanceOfferingUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public String getVmInstanceType() {
        return vmInstanceType;
    }

    public void setVmInstanceType(String vmInstanceType) {
        this.vmInstanceType = vmInstanceType;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    public List<String> getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(List<String> dataDiskOfferingUuids) {
        this.dataDiskOfferingUuids = dataDiskOfferingUuids;
    }

    public String getVmInstanceZoneUuid() {
        return vmInstanceZoneUuid;
    }

    public void setVmInstanceZoneUuid(String vmInstanceZoneUuid) {
        this.vmInstanceZoneUuid = vmInstanceZoneUuid;
    }

    public String getVmInstanceClusterUuid() {
        return vmInstanceClusterUuid;
    }

    public void setVmInstanceClusterUuid(String vmInstanceClusterUuid) {
        this.vmInstanceClusterUuid = vmInstanceClusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getVmInstanceName() {
        return vmInstanceName;
    }

    public void setVmInstanceName(String vmInstanceName) {
        this.vmInstanceName = vmInstanceName;
    }

    public String getVmInstanceDescription() {
        return vmInstanceDescription;
    }

    public void setVmInstanceDescription(String vmInstanceDescription) {
        this.vmInstanceDescription = vmInstanceDescription;
    }

    public String getType() {
        return AutoScalingConstants.AutoScalingTemplate.VmInstance.TYPE;
    }
}
