package org.zstack.zsv.storage.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(
        path = "/ceph-plugin/check",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APICheckCephPluginReply.class
)
public class APICheckCephPluginMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private boolean managementNode = true;

    @APIParam(required = false, resourceType = HostVO.class)
    private List<String> hostUuidList;

    @Deprecated
    @APIParam(required = false)
    private List<String> ipList;

    @APIParam(required = false)
    private List<HostSshParameter> externalHosts;

    public boolean isManagementNode() {
        return managementNode;
    }

    public void setManagementNode(boolean managementNode) {
        this.managementNode = managementNode;
    }

    public List<String> getHostUuidList() {
        return hostUuidList;
    }

    public void setHostUuidList(List<String> hostUuidList) {
        this.hostUuidList = hostUuidList;
    }

    public List<String> getIpList() {
        return ipList;
    }

    public void setIpList(List<String> ipList) {
        this.ipList = ipList;
    }

    public List<HostSshParameter> getExternalHosts() {
        return externalHosts;
    }

    public void setExternalHosts(List<HostSshParameter> externalHosts) {
        this.externalHosts = externalHosts;
    }

    public static APICheckCephPluginMsg __example__() {
        return new APICheckCephPluginMsg();
    }
}
