package org.zstack.header.vm;


import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestResponse(fieldsTo = {"name", "uuid", "hostUuid", "topology"})
public class APIGetVmvNUMATopologyReply extends MessageReply {
    private String name;
    private String uuid;
    private String hostUuid;
    private List<Map<String, Object>> topology;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public void setTopology(List<Map<String, Object>> topology) {
        this.topology = topology;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public List<Map<String, Object>> getTopology() {
        return topology;
    }

    public static APIGetVmvNUMATopologyReply __example__() {
        APIGetVmvNUMATopologyReply reply = new APIGetVmvNUMATopologyReply();

        List<Map<String,Object>> topology = new ArrayList<Map<String,Object>> ();
        Map<String, Object> node = new HashMap<String, Object>();
        node.put("nodeID", "0");
        node.put("phyNodeID", "1");

        List<String> CPUsID = new ArrayList<String>();
        CPUsID.add("0");
        node.put("CPUsID", CPUsID);

        List<String> phyCPUsID = new ArrayList<String>();
        phyCPUsID.add("16");
        node.put("phyCPUsID", phyCPUsID);

        topology.add(node);

        reply.setUuid("f7bae73b9874344b8766dfcdda48ad6e");
        reply.setName("example");
        reply.setHostUuid("f7bae73b9874344b8766dfcdda48ad6e");
        reply.setTopology(topology);
        return reply;
    }

}
