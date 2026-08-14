package org.zstack.zops.api;

import org.zstack.header.rest.RestResponse;
import org.zstack.zops.ChronyServerInfo;
import org.zstack.zops.ChronyServerInfoPair;
import org.zstack.header.message.APIReply;
import org.zstack.zops.HostConnectedStatus;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = "all")
public class APIGetChronyServersReply extends APIReply {
    private List<ChronyServerInfoPair> servers;

    public List<ChronyServerInfoPair> getServers() {
        return servers;
    }

    public void setServers(List<ChronyServerInfoPair> servers) {
        this.servers = servers;
    }

    public static APIGetChronyServersReply __example__() {
        APIGetChronyServersReply reply = new APIGetChronyServersReply();
        List<ChronyServerInfoPair> result = new ArrayList<>();
        ChronyServerInfoPair pair1 = new ChronyServerInfoPair();

        pair1.setInternal(new ChronyServerInfo("172.0.0.1", HostConnectedStatus.Connected));
        pair1.setExternal(new ChronyServerInfo("ntp.test.com", HostConnectedStatus.Connected));

        result.add(pair1);

        reply.setServers(result);
        return reply;
    }
}
