package org.zstack.expon.sdk.nvmf;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExponRestRequest(
        path = "/block/nvmf_client/{clientId}/hosts",
        method = HttpMethod.PUT,
        responseClass = ChangeNvmeClientGroupResponse.class
)
public class ChangeNvmeClientGroupRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String clientId;
    @Param(validValues = {"add", "remove"})
    private String action;
    @Param
    private List<NvmfClient> hosts;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<NvmfClient> getHosts() {
        return hosts;
    }

    public void setHosts(List<NvmfClient> hosts) {
        this.hosts = hosts;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
