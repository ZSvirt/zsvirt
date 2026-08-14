package org.zstack.guesttools;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/update-nic-config",
        isAction = true,
        responseClass = APIUpdateVmNetworkConfigEvent.class,
        method = HttpMethod.PUT
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIUpdateVmNetworkConfigMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(resourceType = VmNicVO.class, nonempty = true)
    private List<String> vmNicUuids;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }

    public static APIUpdateVmNetworkConfigMsg __example__() {
        APIUpdateVmNetworkConfigMsg msg = new APIUpdateVmNetworkConfigMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setVmNicUuids(Collections.singletonList(uuid()));

        return msg;
    }
}
