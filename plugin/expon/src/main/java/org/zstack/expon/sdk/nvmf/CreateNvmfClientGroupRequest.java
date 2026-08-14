package org.zstack.expon.sdk.nvmf;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExponRestRequest(
        path = "/block/nvmf_client",
        method = HttpMethod.POST,
        responseClass = CreateNvmfClientGroupResponse.class
)
public class CreateNvmfClientGroupRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String name;
    @Param(required = false)
    private String description;
    @Param
    private String tianshuId;

    @Param
    private List<NvmfClient> hosts;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTianshuId() {
        return tianshuId;
    }

    public void setTianshuId(String tianshuId) {
        this.tianshuId = tianshuId;
    }

    public List<NvmfClient> getHosts() {
        return hosts;
    }

    public void setHosts(List<NvmfClient> hosts) {
        this.hosts = hosts;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
