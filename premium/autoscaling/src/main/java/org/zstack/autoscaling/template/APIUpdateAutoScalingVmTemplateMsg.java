package org.zstack.autoscaling.template;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * Create by lining at 2020/02/25
 */
@TagResourceType(AutoScalingVmTemplateVO.class)
@RestRequest(
        path = "/autoscaling/vmtemplate/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateAutoScalingTemplateEvent.class
)
public class APIUpdateAutoScalingVmTemplateMsg extends APIMessage {
    @APIParam(resourceType = AutoScalingVmTemplateVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 255, required = false)
    private String vmInstanceName;

    @APIParam(required = false, maxLength = 2048)
    private String vmInstanceDescription;

    @APIParam(resourceType = InstanceOfferingVO.class, required = false)
    private String vmInstanceOfferingUuid;

    @APIParam(resourceType = ImageVO.class, required = false)
    private String imageUuid;

    @APIParam(required = false, resourceType = ClusterVO.class)
    private String vmInstanceClusterUuid;

    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;

    public static APIUpdateAutoScalingVmTemplateMsg __example__() {
        APIUpdateAutoScalingVmTemplateMsg msg = new APIUpdateAutoScalingVmTemplateMsg();

        msg.setUuid(uuid());
        msg.setName("template");
        msg.setVmInstanceName("vm");
        msg.setDescription("for new vm");
        msg.setVmInstanceDescription("vm desc");
        msg.setImageUuid(uuid());
        msg.setVmInstanceOfferingUuid(uuid());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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
}
