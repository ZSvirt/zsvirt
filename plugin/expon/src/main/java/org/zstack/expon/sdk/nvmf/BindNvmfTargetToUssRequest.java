package org.zstack.expon.sdk.nvmf;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExponRestRequest(
        path = "/block/nvmf/bind_uss",
        method = HttpMethod.PUT,
        responseClass = BindNvmfTargetToUssResponse.class
)
public class BindNvmfTargetToUssRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();
    @Param
    private String nvmfId;
    @Param
    private int port;
    @Param
    private List<String> ussGwId;

    public String getNvmfId() {
        return nvmfId;
    }

    public void setNvmfId(String nvmfId) {
        this.nvmfId = nvmfId;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<String> getUssGwId() {
        return ussGwId;
    }

    public void setUssGwId(List<String> ussGwId) {
        this.ussGwId = ussGwId;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
