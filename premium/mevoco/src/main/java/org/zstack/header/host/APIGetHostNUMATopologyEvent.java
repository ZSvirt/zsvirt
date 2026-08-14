package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RestResponse;


import java.util.*;

@RestResponse(fieldsTo = {"name", "uuid", "topology"})
public class APIGetHostNUMATopologyEvent extends APIEvent {
    private String name;
    private String uuid;
    private Map<String, HostNUMANode> topology;

    public APIGetHostNUMATopologyEvent() {
        super(null);
    }

    public APIGetHostNUMATopologyEvent(String apiId) {
        super(apiId);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setTopology(Map<String, HostNUMANode> topology) {
        this.topology = topology;
    }

    public Map<String, HostNUMANode> getTopology() {
        return topology;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }
}
