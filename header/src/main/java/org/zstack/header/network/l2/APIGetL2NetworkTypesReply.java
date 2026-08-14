package org.zstack.header.network.l2;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @apiResult api reply for message :ref:`APIGetL2NetworkTypesMsg`
 * @category l2network
 * @example {
 * "org.zstack.header.network.l2.APIGetL2NetworkTypesReply": {
 * "l2NetworkTypes": [
 * "L2VlanNetwork",
 * "L2NoVlanNetwork"
 * ],
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse(fieldsTo = {"types=l2NetworkTypes"})
public class APIGetL2NetworkTypesReply extends APIReply {
    /**
     * @desc a list of supported l2Network types
     */
    private List<String> l2NetworkTypes;

    public List<String> getL2NetworkTypes() {
        return l2NetworkTypes;
    }

    public void setL2NetworkTypes(List<String> l2NetworkTypes) {
        this.l2NetworkTypes = l2NetworkTypes;
    }
 
    public static APIGetL2NetworkTypesReply __example__() {
        APIGetL2NetworkTypesReply reply = new APIGetL2NetworkTypesReply();
        reply.setL2NetworkTypes(Arrays.asList("L2VlanNetwork", "L2NoVlanNetwork"));
        return reply;
    }

}
