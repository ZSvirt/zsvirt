package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:38 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(fieldsTo = {"types=l3NetworkTypes"})
public class APIGetL3NetworkTypesReply extends APIReply {
    private List<String> l3NetworkTypes;

    public List<String> getL3NetworkTypes() {
        return l3NetworkTypes;
    }

    public void setL3NetworkTypes(List<String> l3NetworkTypes) {
        this.l3NetworkTypes = l3NetworkTypes;
    }
 
    public static APIGetL3NetworkTypesReply __example__() {
        APIGetL3NetworkTypesReply reply = new APIGetL3NetworkTypesReply();
        reply.setL3NetworkTypes(Arrays.asList("L3BasicNetwork"));
        return reply;
    }

}
