package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @api
 * add vm nic to a security group
 *
 * @category security group
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.securitygroup.APIAddVmNicToSecurityGroupMsg": {
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"vmNicUuids": [
"e93a0d92a37c4be7b26a6e565f24b063"
],
"session": {
"uuid": "47bd38c2233d469db97930ab8c71e699"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.securitygroup.APIAddVmNicToSecurityGroupMsg": {
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"vmNicUuids": [
"e93a0d92a37c4be7b26a6e565f24b063"
],
"session": {
"uuid": "47bd38c2233d469db97930ab8c71e699"
},
"timeout": 1800000,
"id": "61f68cba51f8466893194a5af7801ab3",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIAddVmNicToSecurityGroupEvent`
 */
@RestRequest(
        path = "/security-groups/{securityGroupUuid}/vm-instances/nics",
        method = HttpMethod.POST,
        responseClass = APIAddVmNicToSecurityGroupEvent.class,
        parameterName = "params"
)
public class APIAddVmNicToSecurityGroupMsg extends APIMessage {
    /**
     * @desc security group uuid
     */
    @APIParam(resourceType = SecurityGroupVO.class)
    private String securityGroupUuid;
    /**
     * @desc a list of vm nic uuid. See :ref:`VmNicInventory`
     */
    @APIParam(resourceType = VmNicVO.class, nonempty = true)
    private List<String> vmNicUuids;
    
    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }


    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
 
    public static APIAddVmNicToSecurityGroupMsg __example__() {
        APIAddVmNicToSecurityGroupMsg msg = new APIAddVmNicToSecurityGroupMsg();
        msg.setSecurityGroupUuid(uuid());
        msg.setVmNicUuids(asList(uuid()));
        return msg;
    }

}
