package org.zstack.zops.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zops.HostConnectedStatus;
import org.zstack.zops.NetworkReachablePair;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(fieldsTo = "all")
public class APICheckNetworkReachableReply extends APIReply {
    public List<NetworkReachablePair> getResults() {
        return results;
    }

    public void setResults(List<NetworkReachablePair> results) {
        this.results = results;
    }

    private List<NetworkReachablePair> results;

    public static APICheckNetworkReachableReply __example__() {
        APICheckNetworkReachableReply reply = new APICheckNetworkReachableReply();
        List<NetworkReachablePair> result = new ArrayList<>();
        NetworkReachablePair pair1 = new NetworkReachablePair();
        NetworkReachablePair pair2 = new NetworkReachablePair();

        pair1.setSourceHostname("172.0.0.1");
        pair1.setTargetHostname("test1.com");
        pair1.setStatus(HostConnectedStatus.Connected);

        pair1.setSourceHostname("172.0.0.1");
        pair1.setTargetHostname("test2.com");
        pair1.setStatus(HostConnectedStatus.Disconnected);

        result.add(pair1);
        result.add(pair2);

        reply.setResults(result);
        return reply;
    }
}
