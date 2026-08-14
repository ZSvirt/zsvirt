package org.zstack.expon.sdk.nvmf;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;
import org.zstack.expon.sdk.volume.LunResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExponRestRequest(
        path = "/block/nvmf_client/{clientId}/luns",
        method = HttpMethod.PUT,
        responseClass = ChangeVolumeInNvmfClientGroupResponse.class
)
public class ChangeVolumeInNvmfClientGroupRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String clientId;
    @Param(validValues = {"add", "remove"})
    private String action;
    @Param
    private List<LunResource> luns;
    @Param(required = false)
    private List<String> gateways;


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

    public List<LunResource> getLuns() {
        return luns;
    }

    public void setLuns(List<LunResource> luns) {
        this.luns = luns;
    }

    public void setGateways(List<String> gateways) {
        this.gateways = gateways;
    }

    public List<String> getGateways() {
        return gateways;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
