package org.zstack.header.bootstrap;

import org.zstack.header.cluster.APICreateClusterEvent;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(fieldsTo = {"all"})
public class APIBootstrapMiniHostEvent extends APIEvent {
    private String stage;

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public APIBootstrapMiniHostEvent(String apiId) {
        super(apiId);
    }

    public APIBootstrapMiniHostEvent() {
    }

    public static APIBootstrapMiniHostEvent __example__() {
        APIBootstrapMiniHostEvent evt = new APIBootstrapMiniHostEvent();
        return evt;
    }
}
