package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;
import java.util.concurrent.TimeUnit;

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/create-templated-vmInstance",
        method = HttpMethod.POST,
        responseClass = APICreateTemplatedVmInstanceFromVmInstanceEvent.class,
        parameterName = "params"
)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid", updateOnFailure = true)
public class APICreateTemplatedVmInstanceFromVmInstanceMsg extends APIMessage implements APIAuditor {
    @APIParam(nonempty = true)
    private String name;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(required = false, resourceType = ClusterVO.class)
    private String clusterUuid;

    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateTemplatedVmInstanceFromVmInstanceEvent) rsp).getTemplatedVmInstanceInventory().getUuid() : "", VmInstanceVO.class);
    }

    public static APICreateTemplatedVmInstanceFromVmInstanceMsg __example__() {
        APICreateTemplatedVmInstanceFromVmInstanceMsg msg = new APICreateTemplatedVmInstanceFromVmInstanceMsg();
        msg.setName("vm1");
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}
