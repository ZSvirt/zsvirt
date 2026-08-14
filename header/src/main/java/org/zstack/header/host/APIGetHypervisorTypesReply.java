package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * @apiResult api reply for message :ref:`APIGetHypervisorTypesMsg`
 * @example {
 * "org.zstack.header.host.APIGetHypervisorTypesReply": {
 * "hypervisorTypes": [
 * "KVM",
 * "Simulator"
 * ],
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "hypervisorTypes")
public class APIGetHypervisorTypesReply extends APIReply {
    /**
     * @desc a list of hypervisor types supported by zstack
     * @choices - KVM
     * - Simulator
     */
    private List<String> hypervisorTypes;

    public List<String> getHypervisorTypes() {
        return hypervisorTypes;
    }

    public void setHypervisorTypes(List<String> hypervisorTypes) {
        this.hypervisorTypes = hypervisorTypes;
    }
 
    public static APIGetHypervisorTypesReply __example__() {
        APIGetHypervisorTypesReply reply = new APIGetHypervisorTypesReply();


        return reply;
    }

}
