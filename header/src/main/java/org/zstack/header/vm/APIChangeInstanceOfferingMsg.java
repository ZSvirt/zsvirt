package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by frank on 7/16/2015.
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeInstanceOfferingEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIChangeInstanceOfferingMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;
    @APIParam(resourceType = InstanceOfferingVO.class)
    private String instanceOfferingUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }
 
    public static APIChangeInstanceOfferingMsg __example__() {
        APIChangeInstanceOfferingMsg msg = new APIChangeInstanceOfferingMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setInstanceOfferingUuid(uuid());
        return msg;
    }
}
