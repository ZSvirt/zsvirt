package org.zstack.header.network.l2;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * @apiResult api reply for message :ref:`APIGetVSwitchTypesMsg`
 * @category l2network
 * @example {
 * "org.zstack.header.network.l2.APIGetVSwitchTypesReply": {
 * "vSwitchTypes": [
 * "LinuxBridge",
 * "OvsDpdk"
 * ],
 * "success": true
 * }
 * }
 * @since 4.1.0
 */

@RestResponse(fieldsTo = {"types=vSwitchTypes"})
public class APIGetVSwitchTypesReply extends APIReply {
    /**
     * @desc a list of supported vSwitch types
     */
    private List<String> vSwitchTypes;

    public List<String> getVSwitchTypes() {
        return vSwitchTypes;
    }

    public void setVSwitchTypes(List<String> vSwitchTypes) {
        this.vSwitchTypes = vSwitchTypes;
    }

    public static APIGetVSwitchTypesReply __example__() {
        APIGetVSwitchTypesReply reply = new APIGetVSwitchTypesReply();
        reply.setVSwitchTypes(Arrays.asList(L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE, L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK));
        return reply;
    }
}